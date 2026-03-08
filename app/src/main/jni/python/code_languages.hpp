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


#ifndef LIFEOGRAPH_CODE_LANGUAGES_HEADER
#define LIFEOGRAPH_CODE_LANGUAGES_HEADER

#ifndef __ANDROID__
#include "glibmm/regex.h"
#endif

#include "../diaryelements/diarydata.hpp"


namespace LoG
{

// using namespace HELPERS;

struct LangHighlightRules
{
    LangHighlightRules( const char* e ) : expr( e ) {};
    LangHighlightRules( const char* e, Glib::Regex::CompileFlags f ) : expr( e ), flags( f ) {}

    const char*                     expr;
    const Glib::Regex::CompileFlags flags { Glib::Regex::CompileFlags::DEFAULT };
};

using CodeLangMap = std::unordered_map< int, LangHighlightRules >;


static const CodeLangMap CODE_LANGUAGES =
{
// BASH ============================================================================================
    { VT::QT::BASH::I | VT::QT::KEYWORDS,
R"(\b(?:if|then|else|elif|fi|case|esac|for|select|while|until|do|done|in|function|time|coproc|break|continue|return|eval|exec|exit|export|getopts|hash|pwd|readonly|shift|test|times|trap|umask|unset)\b)"
    },
    { VT::QT::BASH::I | VT::QT::COMMENTS,
R"(#.*)"
    },
    { VT::QT::BASH::I | VT::QT::STRINGS,
R"((?:'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"))"
    },

// C++ =============================================================================================
    { VT::QT::CPP::I | VT::QT::KEYWORDS,
R"(\b(?:int|float|double|char|bool|void|auto|const|volatile|static|extern|inline|class|struct|enum|namespace|template|typename|using|public|private|protected|virtual|override|return|if|else|for|while|do|switch|case|break|continue|new|delete|throw|try|catch)\b)"
    },
    { VT::QT::CPP::I | VT::QT::COMMENTS,
R"(//.*)"
// TODO: block comments
    },
    { VT::QT::CPP::I | VT::QT::STRINGS,
R"((?:'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"))"
    },

// FORTRAN =========================================================================================
    { VT::QT::FORTRAN::I | VT::QT::KEYWORDS,
      LangHighlightRules(
R"(\b(?:PROGRAM|END|SUBROUTINE|FUNCTION|MODULE|USE|IMPLICIT|NONE|REAL|INTEGER|LOGICAL|CHARACTER|COMPLEX|PARAMETER|TYPE|DO|ENDDO|IF|THEN|ELSE|ENDIF|SELECT|CASE|ALLOCATE|DEALLOCATE|INTERFACE|CONTAINS|RETURN|STOP|CYCLE|EXIT|PRINT|WRITE|READ|OPEN|CLOSE)\b)",
        Glib::Regex::CompileFlags::CASELESS )
    },
    { VT::QT::FORTRAN::I | VT::QT::COMMENTS,
R"(!.*)"
    },
    { VT::QT::FORTRAN::I | VT::QT::STRINGS,
R"((?:'(?:''|[^'])*'|"(?:\"\"|[^"])*"))"
    },

// GO ==============================================================================================
    { VT::QT::GO::I | VT::QT::KEYWORDS,
R"(\b(?:break|case|chan|const|continue|default|defer|else|fallthrough|for|func|go|goto|if|import|interface|map|package|range|return|select|struct|switch|type|var)\b)"
    },
    { VT::QT::GO::I | VT::QT::COMMENTS,
R"(//.*)"
// TODO: block kcomments R"(/\*[\s\S]*?\*/)"
    },
    { VT::QT::GO::I | VT::QT::STRINGS,
R"((?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'))"
// TODO: Raw strings R"(`[^`]*`)"
    },

// HASKELL =========================================================================================
    { VT::QT::HASKELL::I | VT::QT::KEYWORDS,
R"(\b(?:case|class|data|default|deriving|do|else|if|import|in|infix|infixl|infixr|instance|let|module|newtype|of|then|type|where|qualified|as|hiding|pattern)\b)"
    },
    { VT::QT::HASKELL::I | VT::QT::COMMENTS,
R"(--.*)"
// TODO: comments R"(\{\-[\s\S]*?\-\})"
    },
    { VT::QT::HASKELL::I | VT::QT::STRINGS,
R"((?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'))"
    },

// HTML ============================================================================================
    { VT::QT::HTML::I | VT::QT::KEYWORDS,
      LangHighlightRules(
R"(<[^<]*>)",
// R"(</?[A-Za-z][A-Za-z0-9:-]*>)",
        Glib::Regex::CompileFlags::CASELESS )
    },
    { VT::QT::HTML::I | VT::QT::COMMENTS,
R"(<!--[\s\S]*?-->)"
    },
    { VT::QT::HTML::I | VT::QT::STRINGS,
R"((?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'))"
    },

// TODO: attributes  R"(\b[A-Za-z_:][A-Za-z0-9_.:-]*(?=\s*=))"

// JAVA ============================================================================================
    { VT::QT::JAVA::I | VT::QT::KEYWORDS,
R"(\b(?:abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while|true|false)\b)"
    },
    { VT::QT::JAVA::I | VT::QT::COMMENTS,
R"(//.*)"
    },
    { VT::QT::JAVA::I | VT::QT::STRINGS,
R"((?:'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"))"
    },

// JAVASCRIPT ======================================================================================
    { VT::QT::JAVASCRIPT::I | VT::QT::KEYWORDS,
R"(\b(?:break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|function|if|import|in|instanceof|let|new|return|super|switch|this|throw|try|typeof|var|void|while|with|yield|await|async)\b)"
    },
    { VT::QT::JAVASCRIPT::I | VT::QT::COMMENTS,
R"(//.*)"
// TODO: block comments  R"(/\*[\s\S]*?\*/)"
    },
    { VT::QT::JAVASCRIPT::I | VT::QT::STRINGS,
R"((?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'))"
// TODO: Template strings  R"(`[^`]*`)"
    },

// KOTLIN ==========================================================================================
    { VT::QT::KOTLIN::I | VT::QT::KEYWORDS,
R"(\b(?:as|break|class|continue|do|else|false|for|fun|if|in|interface|is|null|object|package|return|super|this|throw|true|try|typealias|typeof|val|var|when|while|by|catch|constructor|delegate|dynamic|field|file|finally|get|import|init|param|property|receiver|set|setparam|where)\b)"
    },
    { VT::QT::KOTLIN::I | VT::QT::COMMENTS,
R"(//.*)"
// TODO: block comments  R"(/\*[\s\S]*?\*/)"
    },
    { VT::QT::KOTLIN::I | VT::QT::STRINGS,
R"((?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'))"
// TODO: triple-quoted stings R"("""[\s\S]*?""")"
    },

// LISP ============================================================================================
    { VT::QT::LISP::I | VT::QT::KEYWORDS,
R"(\b(?:defun|defmacro|defvar|defparameter|let|let\*|setq|lambda|if|cond|progn|quote|function|loop|return|car|cdr|cons|list|and|or|not|t|nil)\b)"
    },
    { VT::QT::LISP::I | VT::QT::COMMENTS,
R"(;.*)"
    },
    { VT::QT::LISP::I | VT::QT::STRINGS,
R"((?:'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"))"
    },

// LUA =============================================================================================
    { VT::QT::LUA::I | VT::QT::KEYWORDS,
R"(\b(?:and|break|do|else|elseif|end|false|for|function|goto|if|in|local|nil|not|or|repeat|return|then|true|until|while)\b)"
    },
    { VT::QT::LUA::I | VT::QT::COMMENTS,
R"(--.*)"
    },
    { VT::QT::LUA::I | VT::QT::STRINGS,
R"((?:'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"))"
    },

// PASCAL ==========================================================================================
    { VT::QT::PASCAL::I | VT::QT::KEYWORDS,
      LangHighlightRules(
R"(\b(?:PROGRAM|UNIT|INTERFACE|IMPLEMENTATION|BEGIN|END|VAR|TYPE|CONST|PROCEDURE|FUNCTION|CLASS|OBJECT|RECORD|ARRAY|OF|SET|FILE|IF|THEN|ELSE|FOR|TO|DOWNTO|DO|WHILE|REPEAT|UNTIL|CASE|WITH|TRY|EXCEPT|FINALLY|USES|INHERITED|NIL|TRUE|FALSE)\b)",
        Glib::Regex::CompileFlags::CASELESS )
    },
    { VT::QT::PASCAL::I | VT::QT::COMMENTS,
R"(//.*|\{[^}]*\})"
// TODO: { comment }    R"(\{[^}]*\})"
// TODO: (* comment *)   R"(\(\*[^*]*\*+(?:[^)(][^*]*\*+)*\))"
    },
    { VT::QT::PASCAL::I | VT::QT::STRINGS,
R"('(?:''|[^'])*')"
    },

// PERL ============================================================================================
    { VT::QT::PERL::I | VT::QT::KEYWORDS,
R"(\b(?:continue|do|else|elsif|for|foreach|goto|if|last|my|next|our|package|redo|require|sub|undef|unless|until|use|while)\b)"
    },
    { VT::QT::PERL::I | VT::QT::COMMENTS,
R"(#.*)"
    },
    { VT::QT::PERL::I | VT::QT::STRINGS,
R"((?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'))"
    },

// PYTHON ==========================================================================================
    { VT::QT::PYTHON::I | VT::QT::KEYWORDS,
R"(\b(False|None|True|and|as|assert|async|await|break|class|continue|def|del|elif|else|except|finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|pass|raise|return|try|while|with|yield)\b)"
    },
    { VT::QT::PYTHON::I | VT::QT::COMMENTS,
R"(#.*)"
    },
    { VT::QT::PYTHON::I | VT::QT::STRINGS,
R"(['"]{1,3}([^'"\\]|\\.|\\\n)*?['"]{1,3})"
    },

// RUBY ============================================================================================
    { VT::QT::RUBY::I | VT::QT::KEYWORDS,
R"(\b(?:BEGIN|END|alias|and|begin|break|case|class|def|defined\?|do|else|elsif|end|ensure|false|for|if|in|module|next|nil|not|or|redo|rescue|retry|return|self|super|then|true|undef|unless|until|when|while|yield)\b)"
    },
    { VT::QT::RUBY::I | VT::QT::COMMENTS,
R"(#.*)"
    },
    { VT::QT::RUBY::I | VT::QT::STRINGS,
R"((?:'(?:\\.|[^'\\])*'|"(?:\\.|[^"\\])*"|%(?:q|Q)?\{.*?\}))"
    },

// RUST ============================================================================================
    { VT::QT::RUST::I | VT::QT::KEYWORDS,
R"(\b(?:as|break|const|continue|crate|else|enum|extern|false|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|self|Self|static|struct|super|trait|true|type|unsafe|use|where|while|async|await|dyn)\b)"
    },
    { VT::QT::RUST::I | VT::QT::COMMENTS,
R"(//.*)"
// TODO: block comments R"(/\*[\s\S]*?\*/)"
    },
    { VT::QT::RUST::I | VT::QT::STRINGS,
R"((?:"(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*'))"
    },

// SCALA ===========================================================================================
    { VT::QT::SCALA::I | VT::QT::KEYWORDS,
R"(\b(?:abstract|case|catch|class|def|do|else|extends|false|final|finally|for|forSome|if|implicit|import|lazy|macro|match|new|null|object|override|package|private|protected|return|sealed|super|this|throw|trait|true|try|type|val|var|while|with|yield)\b)"
    },
    { VT::QT::SCALA::I | VT::QT::COMMENTS,
R"(//.*)"
// TODO: Block comments  R"(/\*[\s\S]*?\*/)"
    },
    { VT::QT::SCALA::I | VT::QT::STRINGS,
R"("(\\.|[^"\\])*")"
// TODO: Triple-quoted strings  R"("""[\s\S]*?""")"
    },

// SQL =============================================================================================
    { VT::QT::SQL::I | VT::QT::KEYWORDS,
R"(\b(?:SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|JOIN|LEFT|RIGHT|FULL|OUTER|INNER|ON|AS|GROUP|BY|ORDER|LIMIT|OFFSET|HAVING|DISTINCT|CREATE|TABLE|ALTER|DROP|PRIMARY|KEY|FOREIGN|REFERENCES|NOT|NULL|CHECK|DEFAULT|INDEX|VIEW|TRIGGER|AND|OR|IN|IS|LIKE|BETWEEN|UNION|ALL|EXISTS)\b)"
    },
    { VT::QT::SQL::I | VT::QT::COMMENTS,
R"(--.*)"
    },
    { VT::QT::SQL::I | VT::QT::STRINGS,
R"((?:'(?:''|[^'])*'|"(?:\"\"|[^"])*"))"
    },

};

Glib::RefPtr< Glib::Regex >     get_code_lang_regex( int );

} // namespace LoG

#endif
