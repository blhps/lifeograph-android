/***********************************************************************************

    Copyright (C) 2007-2026 Ahmet Öztürk (aoz_2@yahoo.com)

    This file is part of Lifeograph.

    Lifeograph is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Lifeograph is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Lifeograph.  If not, see <http://www.gnu.org/licenses/>.

***********************************************************************************/


#ifndef LIFEOGRAPH_UNDO_HEADER
#define LIFEOGRAPH_UNDO_HEADER


#include <deque>

#ifndef __ANDROID__
#include <glibmm.h>
#endif

#include "helpers.hpp"


namespace LoG {

enum class UndoableType
{   INSERT_TEXT, ERASE_TEXT, MODIFY_TEXT, MODIFY_FORMAT, MODIFY_ENTRY, MOVE_ENTRY };

// UNDOABLE ACTION PARENT
class Undoable {
    public:
                                    Undoable( UndoableType );
        virtual                     ~Undoable() {}

        HELPERS::DateV              get_time() const { return m_time; }

        UndoableType                get_type() const { return m_type; }

#ifdef LIFEOGRAPH_DEBUG_BUILD
        virtual void                print_debug_info()
        { using namespace HELPERS; PRINT_DEBUG( "name = ", get_name() ); }
#endif

        virtual HELPERS::Ustring    get_name() const;

        UndoableType                m_type;

    protected:
        virtual bool                can_absorb( const Undoable* ) const = 0;
        virtual void                absorb( Undoable* ) = 0;
        virtual Undoable*           execute() = 0;

        HELPERS::DateV              m_time;

    private:

    friend class UndoStack;
};

// UNDO STACK
class UndoStack {
    public:
        //UndoStack() {}

        void
        add_action( Undoable* );

        void
        clear();

        bool
        can_undo()
        {
            return( !m_undos.empty() );
        }

        bool
        can_redo()
        {
            return( !m_redos.empty() );
        }

        Undoable*
        get_undo_cur() const
        {
            return( m_undos.empty() ? nullptr : m_undos.front() );
        }
        Undoable*
        get_redo_cur() const
        {
            return( m_redos.empty() ? nullptr : m_redos.front() );
        }

// Below is not needed now. Will be needed after global undo
//        bool                        is_freezed()
//        {
//            return m_freezed;
//        }

        void
        undo();

        void
        redo();

// Below is not needed now. Will be needed after global undo
//        void                        freeze()
//        {
//            m_freezed = true;
//
//        }
//        void                        thaw()
//        {
//            m_freezed = false;
//        }

#ifdef LIFEOGRAPH_DEBUG_BUILD
        void                        print_debug_info()
        {
            using namespace HELPERS;
            PRINT_DEBUG( "------- UNDOS -------" );
            for( auto undoable : m_undos )
                undoable->print_debug_info();
            PRINT_DEBUG( "------- REDOS -------" );
            for( auto undoable : m_redos )
                undoable->print_debug_info();
        }
#endif

    protected:
        std::deque< Undoable* >     m_undos;
        std::deque< Undoable* >     m_redos;
//        bool                        m_freezed{ false };
};

}

#endif

