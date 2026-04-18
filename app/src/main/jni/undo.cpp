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


#include "undo.hpp"


using namespace LoG;


// UNDOABLE
Undoable::Undoable( UndoableType type )
: m_type( type ), m_time( HELPERS::Date::get_now() )
{
}

HELPERS::Ustring
Undoable::get_name() const
{
    switch( m_type )
    {
        case UndoableType::INSERT_TEXT:   return "Insert Text";
        case UndoableType::ERASE_TEXT:    return "Erase Text";
        case UndoableType::MODIFY_TEXT:   return "Modify Text";
        case UndoableType::MODIFY_FORMAT: return "Modify Format";
        default:                          return "Other";
    }
}

// UNDO MANAGER
void
UndoStack::add_action( Undoable *action )
{
    for( int i = m_redos.size()-1; i>= 0; i-- )
        delete m_redos[ i ];

    m_redos.clear();

    if( ! m_undos.empty() )
        if( m_undos.front()->can_absorb( action ) )
        {
            m_undos.front()->absorb( action );
            delete action;
            return;
        }
    m_undos.push_front( action );
}

void
UndoStack::clear()
{
    int i;
    for( i = m_undos.size()-1; i>= 0; i-- )
        delete m_undos[ i ];

    for( i = m_redos.size()-1; i>= 0; i-- )
        delete m_redos[ i ];

    m_undos.clear();
    m_redos.clear();
}

void
UndoStack::undo()
{
    if( ! m_undos.empty() )
    {
        //m_freezed = true;
        m_redos.push_front( m_undos.front()->execute() );
        delete m_undos.front();
        m_undos.pop_front();
        //m_freezed = false;
    }
}

void
UndoStack::redo()
{
    if( ! m_redos.empty() )
    {
        //m_freezed = true;
        m_undos.push_front( m_redos.front()->execute() );
        delete m_redos.front();
        m_redos.pop_front();
        //m_freezed = false;
    }
}
