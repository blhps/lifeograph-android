#ifndef FUZZY_FINDER_HEADER
#define FUZZY_FINDER_HEADER

#include <algorithm>
#include <vector>

#ifndef __ANDROID__
#include <glibmm/ustring.h>
#endif

#include "helpers.hpp"

namespace HELPERS
{

struct FuzzyMatch
{
    Ustring text;
    int     idx       { -1 };
    int     score     { 0 };
    bool    is_exact  { false };

    bool
    operator>( const FuzzyMatch& other ) const
    {
        return score > other.score;
    }
};

class FuzzyFinder
{
public:
    // match query against candidate, return true if matched with score
    static bool
    match( const Ustring& query, const Ustring& candidate, int& score )
    {
        if( query.empty() )
        {
            score = 0;
            return true;
        }

        auto q = STR::lowercase( query );
        auto c = STR::lowercase( candidate );

        size_t qi = 0, ci = 0;
        int consecutive = 0;
        score = 0;

        while( qi < q.length() && ci < c.length() )
        {
            if( q[ qi ] == c[ ci ] )
            {
                score += ( ci == 0 ) ? 30 : 10;                     // start bonus
                if( consecutive > 0 )                 score += 15;  // consecutive bonus
                if( query[ qi ] == candidate[ ci ] )  score += 5;   // case match bonus
                consecutive++;
                qi++;
            }
            else
            {
                consecutive = 0;
                score -= 2; // Gap penalty
            }
            ci++;
        }

        if( qi < q.length() )
            score -= ( q.length() - qi ); // penalty for exceeding query

        return( score > 0 );
    }

    // search candidates and return sorted matches
    static std::vector< FuzzyMatch >
    search( const Ustring& query, const VecUstrings& candidates )
    {
        std::vector< FuzzyMatch > matches;
        int idx { 0 };
        matches.reserve( candidates.size() / 4 );
        int score_max;

        // calculate max score:
        match( query, query, score_max );

        for( const auto& candidate : candidates )
        {
            int score;
            if( match( query, candidate, score ) )
                matches.push_back( { candidate, idx, score, score == score_max } );
            ++idx;
        }

        std::sort( matches.begin(), matches.end(), std::greater< FuzzyMatch >() );

        return matches;
    }

    // convenience: search and return the first match
    static FuzzyMatch
    search_simple( const Ustring& query, const VecUstrings& candidates )
    {
        auto matches = search( query, candidates );
        return matches.empty() ? FuzzyMatch() : matches[ 0 ];
    }
};

} // end of namespace HELPERS

#endif // FUZZY_FINDER_HEADER
