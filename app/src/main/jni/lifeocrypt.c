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


#include <jni.h>

#include "helpers.h"


JNIEXPORT jboolean JNICALL
Java_net_sourceforge_lifeograph_Diary_initCipher( JNIEnv* env, jobject obj )
{
    return Cipher_init();
}

JNIEXPORT jstring JNICALL
Java_net_sourceforge_lifeograph_Diary_decryptBuffer( JNIEnv* env,
                                                     jobject obj,
                                                     jstring j_passphrase,
                                                     jbyteArray j_salt,
                                                     jbyteArray j_buffer,
                                                     jint size,
                                                     jbyteArray j_iv )
{
    unsigned char* key;
    const char* passphrase = ( *env )->GetStringUTFChars( env, j_passphrase, NULL );
    jbyte* salt = ( *env )->GetByteArrayElements( env, j_salt, NULL );

    Cipher_expand_key( passphrase, salt, &key );

    jbyte* buffer = ( *env )->GetByteArrayElements( env, j_buffer, NULL );
    jbyte* iv = ( *env )->GetByteArrayElements( env, j_iv, NULL );

    Cipher_decrypt_buffer( buffer, size, key, iv );

    free( key );

    ( *env )->ReleaseByteArrayElements( env, j_salt, salt, 0 );
    ( *env )->ReleaseByteArrayElements( env, j_iv, iv, 0 );

    /*jbyteArray j_buffer_out = ( *env )->NewByteArray( env, size );  // allocate
    if( !j_buffer_out )
        return NULL;

    ( *env )->SetByteArrayRegion( env, j_buffer_out, 0, size, buffer );  // copy

    ( *env )->ReleaseByteArrayElements( env, j_buffer, buffer, 0 );

    return j_buffer_out;*/

    jstring output;
    // cannot check the '\n' due to multi-byte char case
    if( buffer[ 0 ] == passphrase[ 0 ] ) //&& buffer[ 1 ] == '\n' )
        output = ( *env )->NewStringUTF( env, buffer );
    else
        output = ( *env )->NewStringUTF( env, "XX" );

    ( *env )->ReleaseByteArrayElements( env, j_buffer, buffer, 0 );

    return output;
}

JNIEXPORT jbyteArray JNICALL
Java_net_sourceforge_lifeograph_Diary_encryptBuffer( JNIEnv* env,
                                                     jobject obj,
                                                     jstring j_passphrase,
                                                     jbyteArray j_buffer,
                                                     jint size )
{
    const char* passphrase = ( *env )->GetStringUTFChars( env, j_passphrase, NULL );
    jbyte* buffer = ( *env )->GetByteArrayElements( env, j_buffer, NULL );
    unsigned char* key;
    unsigned char* salt;
    unsigned char* iv;

    Cipher_create_new_key( passphrase, &salt, &key );
    Cipher_create_iv( &iv );

    Cipher_encrypt_buffer( buffer, size, key, iv );

    // allocate
    jbyteArray j_buffer_out = ( *env )->NewByteArray( env, cSALT_SIZE + cIV_SIZE + size );
    if( !j_buffer_out )
        return NULL;

    // copy
    ( *env )->SetByteArrayRegion( env, j_buffer_out, 0, cSALT_SIZE, salt );
    ( *env )->SetByteArrayRegion( env, j_buffer_out, cSALT_SIZE, cIV_SIZE, iv );
    ( *env )->SetByteArrayRegion( env, j_buffer_out, cSALT_SIZE + cIV_SIZE, size, buffer );

    // free
    ( *env )->ReleaseByteArrayElements( env, j_buffer, buffer, 0 );

    free( key );
    free( salt );
    free( iv );

    return j_buffer_out;
}
