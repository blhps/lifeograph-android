/***********************************************************************************

    Copyright (C) 2007-2015 Ahmet Öztürk (aoz_2@yahoo.com)

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


#ifndef LIFEOGRAPH_HELPERS_HEADER
#define LIFEOGRAPH_HELPERS_HEADER


#include <stdbool.h>
#include <gcrypt.h>


// ENCRYPTION ======================================================================================
    static const int    cCIPHER_ALGORITHM   = GCRY_CIPHER_AES256;
    static const int    cCIPHER_MODE        = GCRY_CIPHER_MODE_CFB;
    static const int    cIV_SIZE            = 16; // = 128 bits
    static const int    cSALT_SIZE          = 16; // = 128 bits
    static const int    cKEY_SIZE           = 32; // = 256 bits
    static const int    cHASH_ALGORITHM     = GCRY_MD_SHA256;

    bool                Cipher_init();

    bool                Cipher_create_iv( unsigned char** );

    bool                Cipher_expand_key( char const*,
                                           const unsigned char*,
                                           unsigned char** );

    bool                Cipher_create_new_key( char const*,
                                               unsigned char**,
                                               unsigned char** );

    bool                Cipher_encrypt_buffer( unsigned char*,
                                               unsigned int,
                                               const unsigned char*,
                                               const unsigned char* );

    bool                Cipher_decrypt_buffer( unsigned char*,
                                               unsigned int,
                                               const unsigned char*,
                                               const unsigned char* );

/*struct CipherBuffers
{
    CipherBuffers()
    :   buffer( NULL ), salt( NULL ), iv( NULL ), key( NULL ) {}

    unsigned char* buffer;
    unsigned char* salt;
    unsigned char* iv;
    unsigned char* key;

    void clear()
    {
        if( buffer ) delete[] buffer;
        if( salt ) delete[] salt;
        if( iv ) delete[] iv;
        if( key ) delete[] key;
    }
};*/

#endif
