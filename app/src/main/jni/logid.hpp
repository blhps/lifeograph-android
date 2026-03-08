/***********************************************************************************

    Copyright (C) 2007-2026 Ahmet Öztürk (aoz_2@yahoo.com)

    Parts of this file are loosely based on an example gcrypt program
    on http://punkroy.drque.net/

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

#pragma once

#include <charconv>
#include <cstdint>

#include "helpers.hpp"


namespace LoG
{

// using LoGID32 = uint32_t; // LoGID base
// using LoGID64 = uint64_t; // LoGID base

// we use enum to able to expose m_id to enable switch usage:
enum class LoGID32 : uint32_t {};
enum class LoGID64 : uint64_t {};

// MAIN ID CLASS (ENUM) ============================================================================
class LoGID
{
public:
    constexpr
    LoGID() : m_id( 0 ) {}

    constexpr
    LoGID( LoGID32 id ) : m_id( static_cast< uint32_t >( id ) ) {}

    explicit
    LoGID( const std::string& str )
    : m_id( std::stoul( str ) ) {}

    constexpr
    operator LoGID64() const noexcept { return LoGID64( m_id ); } // this is for switches to work

    void
    set( uint32_t val ) { m_id = val; }

    constexpr bool
    is_stock() const;

    constexpr uint64_t // shared with 64 bit version
    get_raw() const { return m_id; }

    // std::string
    // get_str() const { return get_as_str( *this ); }
    //
    // static std::string
    // get_as_str( const LoGID& id )
    // {
    //     char buffer[ 11 ];
    //     // NOTE: this is supposedly faster than a plain std::to_string()
    //     auto result { std::to_chars( buffer, buffer + sizeof( buffer ), id.get_raw() ) };
    //     return std::string( buffer, result.ptr );
    // }
    std::string
    get_str() const
    {
        char buffer[ 21 ];
        // NOTE: this is supposedly faster than a plain std::to_string()
        auto result { std::to_chars( buffer, buffer + sizeof( buffer ), m_id ) };
        return std::string( buffer, result.ptr );
    }

    constexpr bool
    operator==( const LoGID& other ) const { return( m_id == other.m_id ); }

    constexpr bool
    operator<( const LoGID& other ) const { return m_id < other.m_id; }
    constexpr bool
    operator>( const LoGID& other ) const { return m_id > other.m_id; }

    constexpr bool
    operator>=( const LoGID& other ) const { return m_id >= other.m_id; }
    constexpr bool
    operator<=( const LoGID& other ) const { return m_id <= other.m_id; }

    template< typename T >
    T
    cast() const { return T{ m_id }; }
    // NOTE: static or dynamic casts are not an option here

protected:
    constexpr explicit
    LoGID( uint64_t id ) : m_id( id ) {} // only for internal conversions

    uint64_t m_id;
};

class LoGIDF : public LoGID
{
public:
    using LoGID::LoGID;

    LoGIDF( enum LoGID32 ) = delete;

    LoGIDF( enum LoGID64 id ) : LoGID( static_cast< uint64_t >( id ) ) {}

    LoGIDF( const LoGID& id_lo, const LoGID& id_hi )
    : LoGID( ( id_lo.get_raw() & 0xFFFF'FFFF ) | ( id_hi.get_raw() << 32 ) ) {}

    constexpr
    operator LoGID64() const noexcept { return LoGID64( m_id ); } // this is for switches to work

    constexpr LoGID
    get_lo() const { return( LoGID( LoGID32( m_id & 0xFFFF'FFFF ) ) ); }

    constexpr LoGID
    get_hi() const { return( LoGID( LoGID32( m_id >> 32 ) ) ); }

    constexpr void
    set_lo( uint32_t lo_bits ) { m_id = ( ( m_id & 0xFFFFFFFF'00000000 ) | lo_bits ); }

    constexpr void
    set_hi( uint64_t hi_bits ) { m_id = ( ( m_id & 0xFFFFFFFF ) | ( hi_bits << 32 ) ); } 
};

// DEFINITIONS =====================================================================================
#define DECLARE_LOGID_SUBTYPE( Base, Type ) \
    namespace D { \
    struct Type : public Base \
    { using Base::Base; \
      constexpr Type( const LoGID& other ) : Base( other.get_raw() ) {} }; \
    }

DECLARE_LOGID_SUBTYPE( LoGID, DEID );
DECLARE_LOGID_SUBTYPE( LoGIDF, DEIDF ); // 64 bit full ID
DECLARE_LOGID_SUBTYPE( LoGID, CSTR );
DECLARE_LOGID_SUBTYPE( LoGID, PROP );

// Expand definitions into inline constexpr objects:
#define XS(Type, Name, Value, Str) \
    namespace Type{ \
    inline constexpr D::Type Name{ static_cast< LoGID32 >( Value ) }; \
    }

#define XE(Type, Name, Value) \
    namespace Type{ \
    inline constexpr D::Type Name{ static_cast< LoGID32 >( Value ) }; \
    }

#include "stock_ids.dict"

#undef XS
#undef XE

constexpr bool
LoGID::is_stock() const { return *this < DEID::MIN_ACTUAL; }

// HASHER ==========================================================================================
struct LoGIDHasher
{
    template < typename T >
    size_t
    operator()( const T& obj ) const noexcept
    { return std::hash< uint64_t >{}( obj.get_raw() ); }
};

// HELPERS AND CONTAINERS ==========================================================================
const char* get_sstr( const LoGID& );

inline const char*
get_sstr_i( const LoGID& id ) { return _( get_sstr( id ) ); }

using StringKeyValuePair  = std::pair< const LoGID, const HELPERS::Ustring >;
using SKVVec              = std::vector< StringKeyValuePair >;

using VecDEIDs            = std::vector< D::DEID >;
using SetLoGIDs           = std::set< LoGID >;

} // end of namespace LoG
