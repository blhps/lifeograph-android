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

package net.sourceforge.lifeograph;

public class ParserPara extends ParserText
{
    public void
    parse( Paragraph para ) {
        m_p2para_cur = para;
        super.parse( 0, para.get_size() );
    }

    @Override
    protected char
    get_char_at( int i ) {
        return m_p2para_cur.m_text.charAt( i );
    }

    String
    get_substr( int begin, int end ) {
        return m_p2para_cur.m_text.substring( begin, end );
    }

    @Override
    void
    apply_heading() {
        // if() check can be removed when apply_heading calls are made in a more standardized way
        if( m_p2para_cur.m_para_no == 0 )
            m_p2para_cur.m_heading_level = 3;
    }

    @Override
    void
    apply_subheading() {
        m_p2para_cur.m_heading_level = 2;
    }

    @Override
    void
    apply_subsubheading() {
        m_p2para_cur.m_heading_level = 1;
    }

    @Override
    void
    apply_link() {
        if( m_recipe_cur.m_id == RID_DATE )
            m_p2para_cur.m_date = m_date_last.m_date;
    }

    @Override
    void
    apply_check_unf() {
        m_p2para_cur.m_status = DiaryElement.ES_TODO;
    }

    @Override
    void
    apply_check_prg() {
        m_p2para_cur.m_status = DiaryElement.ES_PROGRESSED;
    }

    @Override
    void
    apply_check_fin() {
        m_p2para_cur.m_status = DiaryElement.ES_DONE;
    }

    @Override
    void
    apply_check_ccl() {
        m_p2para_cur.m_status = DiaryElement.ES_CANCELED;
    }

    @Override
    void
    apply_inline_tag() {
        // m_pos_mid is used to determine if the name part or the value type is being applied
        final int pos_name_end = m_recipe_cur.m_pos_mid > 0 ?
                                               m_recipe_cur.m_pos_mid - 1 : m_pos_cur - 1;
        final String tag_name = get_substr( m_recipe_cur.m_pos_bgn + 1, pos_name_end );

        // the value (BEAWARE that the last reference overrides previous ones within a paragraph):
        if( m_recipe_cur.m_pos_mid > 0 ) {
            if( m_pos_extra_1 > m_recipe_cur.m_pos_bgn ) { // has planned value
                final int pos_value_bgn = m_recipe_cur.m_pos_mid + 1;
                final double v_real = Lifeograph.getDouble(
                        get_substr( pos_value_bgn, m_pos_extra_1 ) );
                final double v_plan = Lifeograph.getDouble(
                        get_substr( m_pos_extra_1 + 1, m_pos_extra_2 + 1 ) );
                m_p2para_cur.set_tag( tag_name, v_real, v_plan );
            }
            else
                m_p2para_cur.set_tag( tag_name,
                                      Lifeograph.getDouble( get_substr( m_recipe_cur.m_pos_mid + 1 ,
                                                                        m_pos_cur ) ) );
        }
        else
            m_p2para_cur.set_tag( tag_name, 1.0 );
    }
}
