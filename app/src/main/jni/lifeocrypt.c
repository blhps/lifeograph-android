/***********************************************************************************

    Copyright (C) 2007-2021 Ahmet Öztürk (aoz_2@yahoo.com)

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
#include <stdlib.h>
#include <string.h>

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

    Cipher_expand_key( passphrase, ( const unsigned char* ) salt, &key );

    jbyte* buffer = ( *env )->GetByteArrayElements( env, j_buffer, NULL );
    jbyte* iv = ( *env )->GetByteArrayElements( env, j_iv, NULL );

    Cipher_decrypt_buffer( ( unsigned char* ) buffer, size, key, ( const unsigned char* ) iv );

    free( key );
    ( *env )->ReleaseByteArrayElements( env, j_salt, salt, JNI_ABORT );
    ( *env )->ReleaseByteArrayElements( env, j_iv, iv, JNI_ABORT );
    ( *env )->ReleaseByteArrayElements( env, j_buffer, buffer, JNI_ABORT );
    ( *env )->ReleaseStringUTFChars( env, j_passphrase, passphrase );

    int size_dec_buf = 0;

    // EOF DETECTION: SOMEWHAT UGLY CODE
    for( ; size_dec_buf < size - 1; size_dec_buf++ )
    {
         if( buffer[ size_dec_buf ] == 0 )
            break;
    }

    char* dec_buf = malloc( size_dec_buf + 1 );
    memcpy( dec_buf, buffer, size_dec_buf );
    dec_buf[ size_dec_buf ] = 0; // terminating zero

    jstring output;
    // cannot check the '\n' due to multi-byte char case
    if( dec_buf[ 0 ] == passphrase[ 0 ] ) //&& buffer[ 1 ] == '\n' )
        output = (*env)->NewStringUTF( env, dec_buf );
    else
        output = (*env)->NewStringUTF( env, "XX" );

    free( dec_buf );

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

    Cipher_encrypt_buffer( ( unsigned char* ) buffer, size, key, iv );

    // allocate
    jbyteArray j_buffer_out = ( *env )->NewByteArray( env, cSALT_SIZE + cIV_SIZE + size );
    if( !j_buffer_out )
    {
        ( *env )->ReleaseByteArrayElements( env, j_buffer, buffer, JNI_ABORT );
        ( *env )->ReleaseStringUTFChars( env, j_passphrase, passphrase );
        free( key );
        free( salt );
        free( iv );
        return NULL;
    }

    // copy
    ( *env )->SetByteArrayRegion( env, j_buffer_out, 0, cSALT_SIZE, ( const jbyte* ) salt );
    ( *env )->SetByteArrayRegion( env, j_buffer_out, cSALT_SIZE, cIV_SIZE, ( const jbyte* ) iv );
    ( *env )->SetByteArrayRegion( env, j_buffer_out, cSALT_SIZE + cIV_SIZE, size, buffer );

    // free
    ( *env )->ReleaseByteArrayElements( env, j_buffer, buffer, 0 );
    ( *env )->ReleaseStringUTFChars( env, j_passphrase, passphrase );

    free( key );
    free( salt );
    free( iv );

    return j_buffer_out;
}
