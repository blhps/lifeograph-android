/* *********************************************************************************

    Copyright (C) 2012-2021 Ahmet Öztürk (aoz_2@yahoo.com)

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

package net.sourceforge.lifeograph.helpers;

public enum Result {
    OK, ABORTED, SUCCESS, FAILURE, COULD_NOT_START, /*COULD_NOT_FINISH,*/ WRONG_PASSWORD,
    /*APPARENTLY_ENCRYTED_FILE, APPARENTLY_PLAIN_FILE,*/
    INCOMPATIBLE_FILE_OLD, INCOMPATIBLE_FILE_NEW, CORRUPT_FILE,
    FILE_NOT_FOUND, FILE_NOT_READABLE, FILE_NOT_WRITABLE, FILE_LOCKED
}
