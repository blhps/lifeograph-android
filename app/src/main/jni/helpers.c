/***********************************************************************************

    Copyright (C) 2007-2015 Ahmet Öztürk (aoz_2@yahoo.com)

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


#include "helpers.h"


// ENCRYPTION ======================================================================================
bool
Cipher_init()
{
    // http://www.gnupg.org/documentation/manuals/gcrypt/Initializing-the-library.html

    // initialize subsystems:
    if( ! gcry_check_version( NULL ) )  // TODO: check version
    {
        //print_error( "Libgcrypt version mismatch" );
        return false;
    }

    // disable secure memory
    gcry_control( GCRYCTL_DISABLE_SECMEM, 0 );

    // MAYBE LATER:
    /*
    // suppress warnings
    gcry_control( GCRYCTL_SUSPEND_SECMEM_WARN );

    // allocate a pool of 16k secure memory. this makes the secure memory...
    // ...available and also drops privileges where needed
    gcry_control( GCRYCTL_INIT_SECMEM, 16384, 0 );

    // resume warnings
    gcry_control( GCRYCTL_RESUME_SECMEM_WARN );
    */

    // tell Libgcrypt that initialization has completed
    gcry_control( GCRYCTL_INITIALIZATION_FINISHED, 0 );

    return true;
}

bool
Cipher_create_iv( unsigned char** iv )
{
    // (Allocate memory for and fill with strong random data)
    *iv = ( unsigned char* ) gcry_random_bytes( cIV_SIZE, GCRY_STRONG_RANDOM );

    if( ! *iv )
        //throw Error( "Unable to create IV" );
        return false;

    return true;
}

bool
Cipher_expand_key( const char* passphrase,
                   const unsigned char* salt,
                   unsigned char** key )
{
    gcry_md_hd_t hash;
    gcry_error_t error = 0;
    int hashdigestsize;
    unsigned char* hashresult;

    // OPEN MESSAGE DIGEST ALGORITHM
    error = gcry_md_open( &hash, cHASH_ALGORITHM, 0 );
    if( error )
        //throw Error( "Unable to open message digest algorithm: %s" ); //, gpg_strerror( Error ) );
        return false;

    // RETRIVE DIGEST SIZE
    hashdigestsize = gcry_md_get_algo_dlen( cHASH_ALGORITHM );

    // ADD SALT TO HASH
    gcry_md_write( hash, salt, cSALT_SIZE );

    // ADD PASSPHRASE TO HASH
    gcry_md_write( hash , passphrase , strlen( passphrase ) );

    // FETCH DIGEST (THE EXPANDED KEY)
    hashresult = gcry_md_read( hash , cHASH_ALGORITHM );

    if( ! hashresult )
    {
        gcry_md_close( hash );
        //throw Error( "Unable to finalize key" );
        return false;
    }

    // ALLOCATE MEMORY FOR KEY
    // can't use the 'HashResult' because those resources are freed after the
    // hash is closed
    *key = malloc( cKEY_SIZE ); //new unsigned char[ cKEY_SIZE ];
    if( ! key )
    {
        gcry_md_close( hash );
        //throw Error( "Unable to allocate memory for key" );
        return false;
    }

    // DIGEST SIZE SMALLER THEN KEY SIZE?
    if( hashdigestsize < cKEY_SIZE )
    {
        // PAD KEY WITH '0' AT THE END
        memset( *key , 0 , cKEY_SIZE );

        // COPY EVERYTHING WE HAVE
        memcpy( *key , hashresult , hashdigestsize );
    }
    else
        // COPY ALL THE BYTES WE'RE USING
        memcpy( *key , hashresult , hashdigestsize );

    // FINISHED WITH HASH
    gcry_md_close( hash );

    return true;
}

// create new expanded key
bool
Cipher_create_new_key( char const * passphrase,
                        unsigned char** salt,
                        unsigned char** key )
{ /*
    // ALLOCATE MEMORY FOR AND FILL WITH STRONG RANDOM DATA
    *salt = ( unsigned char* ) gcry_random_bytes( cSALT_SIZE, GCRY_STRONG_RANDOM );

    if( ! *salt )
        //throw Error( "Unable to create salt value" );
        return false;

    expand_key( passphrase, *salt, key );
    */return true;
}

bool
Cipher_encrypt_buffer( unsigned char* buffer,
                        unsigned int size,
                        const unsigned char* key,
                        const unsigned char* iv )
{/*
    gcry_cipher_hd_t    cipher;
    gcry_error_t        error = 0;

    error = gcry_cipher_open( &cipher, cCIPHER_ALGORITHM, cCIPHER_MODE, 0 );

    if( error )
        throw Error( "unable to initialize cipher: " ); // + gpg_strerror( Error ) );

    // GET KEY LENGTH
    int cipherKeyLength = gcry_cipher_get_algo_keylen( cCIPHER_ALGORITHM );
    if( ! cipherKeyLength )
        throw Error( "gcry_cipher_get_algo_keylen failed" );

    // SET KEY
    error = gcry_cipher_setkey( cipher, key, cipherKeyLength );
    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Cipher key setup failed: %s" ); //, gpg_strerror( Error ) );
    }

    // SET INITILIZING VECTOR (IV)
    error = gcry_cipher_setiv( cipher, iv, cIV_SIZE );
    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Unable to setup cipher IV: %s" );// , gpg_strerror( Error ) );
    }

    // ENCRYPT BUFFER TO SELF
    error = gcry_cipher_encrypt( cipher, buffer, size, NULL, 0 );

    if( error )
    {
        gcry_cipher_close( cipher );
        throw Error( "Encrption failed: %s" ); // , gpg_strerror( Error ) );
    }

    gcry_cipher_close( cipher );

    */return true;
}

bool
Cipher_decrypt_buffer( unsigned char* buffer,
                       unsigned int size,
                       const unsigned char* key,
                       const unsigned char* iv )
{
    gcry_cipher_hd_t cipher;
    gcry_error_t error = 0;

    error = gcry_cipher_open( &cipher, cCIPHER_ALGORITHM, cCIPHER_MODE, 0 );

    if( error )
        //throw Error( "Unable to initialize cipher: " ); // + gpg_strerror( Error ) );
        return false;

    // GET KEY LENGTH
    int cipherKeyLength = gcry_cipher_get_algo_keylen( cCIPHER_ALGORITHM );
    if( ! cipherKeyLength )
        //throw Error( "gcry_cipher_get_algo_keylen failed" );
        return false;

    // SET KEY
    error = gcry_cipher_setkey( cipher, key, cipherKeyLength );
    if( error )
    {
        gcry_cipher_close( cipher );
        //throw Error( "Cipher key setup failed: %s" ); //, gpg_strerror( Error ) );
        return false;
    }

    // SET IV
    error = gcry_cipher_setiv( cipher, iv, cIV_SIZE );
    if( error )
    {
        gcry_cipher_close( cipher );
        //throw Error( "Unable to setup cipher IV: %s" );// , gpg_strerror( Error ) );
        return false;
    }

    // DECRYPT BUFFER TO SELF
    error = gcry_cipher_decrypt( cipher, buffer, size, NULL, 0 );

    if( error )
    {
        gcry_cipher_close( cipher );
        //throw Error( "Encryption failed: %s" ); // , gpg_strerror( Error ) );
        return false;
    }

    gcry_cipher_close( cipher );

    return true;
}
