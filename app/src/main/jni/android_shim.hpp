/***********************************************************************************

    Copyright (C) 2021 Ahmet Öztürk (aoz_2@yahoo.com)

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

#ifndef LIFEOGRAPH_ANDROID_SHIM_HPP
#define LIFEOGRAPH_ANDROID_SHIM_HPP

#include <string>
#include <vector>
#include <list>
#include <map>
#include <unordered_map>
#include <memory>
#include <functional>
#include <algorithm>
#include <cmath>
#include <limits>
#include <stdexcept>
#include <cctype>
#include <sstream>

#include <re2/re2.h>

#include <android/log.h>

#define LOG_TAG "LFO"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

typedef size_t gsize;

#define PANGO_SCALE 1024

// Minimal Glib/Gtk stubs for Android build
namespace Glib {
    enum class NormalizeMode {
        DEFAULT,
        DEFAULT_COMPOSE,
        ALL,
        ALL_COMPOSE
    };

    class ustring : public std::string {
    public:
        using std::string::string;
        using std::string::operator+=;   // keep char, const char*, const std::string&, initializer_list overloads visible
        using std::string::push_back;    // keep push_back(char) visible
        ustring(const std::string& s) : std::string(s) {}
        ustring(const char* s) : std::string(s ? s : "") {}
        ustring() = default;

        [[nodiscard]] size_type length() const {
            size_type count = 0;
            for (size_type i = 0; i < std::string::length(); ++i) {
                unsigned char c = static_cast<unsigned char>(std::string::operator[](i));
                if ((c & 0b11000000) != 0b10000000)
                    ++count;
            }
            return count;
        }

        ustring substr(size_type pos, size_type n = std::string::npos) const {
            size_type start_byte = char_to_byte_idx(pos);
            if (n == std::string::npos) {
                return ustring(std::string::substr(start_byte));
            }

            size_type end_byte = char_to_byte_idx(pos + n);
            return ustring(std::string::substr(start_byte, end_byte - start_byte));
        }

        char32_t operator[](size_type char_pos) const {
            size_type byte_idx = char_to_byte_idx(char_pos);
            if (byte_idx >= std::string::length()) return 0;

            unsigned char c1 = static_cast<unsigned char>(std::string::operator[](byte_idx));
            if (c1 < 0x80) return c1;
            if ((c1 & 0xE0) == 0xC0) {
                if (byte_idx + 1 >= std::string::length()) return c1;
                return ((c1 & 0x1F) << 6) | (static_cast<unsigned char>(std::string::operator[](byte_idx + 1)) & 0x3F);
            }
            if ((c1 & 0xF0) == 0xE0) {
                if (byte_idx + 2 >= std::string::length()) return c1;
                return ((c1 & 0x0F) << 12) | ((static_cast<unsigned char>(std::string::operator[](byte_idx + 1)) & 0x3F) << 6) | (static_cast<unsigned char>(std::string::operator[](byte_idx + 2)) & 0x3F);
            }
            if ((c1 & 0xF8) == 0xF0) {
                if (byte_idx + 3 >= std::string::length()) return c1;
                return ((c1 & 0x07) << 18) | ((static_cast<unsigned char>(std::string::operator[](byte_idx + 1)) & 0x3F) << 12) | ((static_cast<unsigned char>(std::string::operator[](byte_idx + 2)) & 0x3F) << 6) | (static_cast<unsigned char>(std::string::operator[](byte_idx + 3)) & 0x3F);
            }
            return c1;
        }

        char32_t at(size_type char_pos) const {
            if (char_pos >= length()) throw std::out_of_range("ustring::at");
            return (*this)[char_pos];
        }

        ustring& operator+=(char32_t c) {
            append_unichar(c);
            return *this;
        }

        ustring& push_back(char32_t c) {
            append_unichar(c);
            return *this;
        }

        size_type size() const { return length(); }

        size_type bytes() const { return std::string::length(); }

        // helper to convert character index to byte offset:
        size_type char_to_byte_idx(size_type char_pos) const {
            size_type byte_idx = 0;
            size_type current_char = 0;
            while (current_char < char_pos && byte_idx < std::string::length()) {
                unsigned char c = static_cast<unsigned char>(std::string::operator[](byte_idx));
                // UTF-8 lead byte check:
                // 0xxxxxxx (ASCII) or 11xxxxxx (Lead byte of multi-byte)
                if ((c & 0b11000000) != 0b10000000) {
                    current_char++;
                }
                byte_idx++;
                // move past continuation bytes
                while (byte_idx < std::string::length() &&
                       (static_cast<unsigned char>(std::string::operator[](byte_idx)) & 0b11000000) == 0b10000000) {
                    byte_idx++;
                }
            }
            return byte_idx;
        }

        // Override insert to use character positions
        ustring& insert(size_type char_pos, const std::string& str) {
            std::string::insert(char_to_byte_idx(char_pos), str);
            return *this;
        }

        // You likely need to override erase as well
        ustring& erase(size_type char_pos, size_type n_chars) {
            size_type start_byte = char_to_byte_idx(char_pos);
            size_type end_byte = char_to_byte_idx(char_pos + n_chars);
            std::string::erase(start_byte, end_byte - start_byte);
            return *this;
        }

        class const_iterator {
        public:
            const_iterator(const char* p, const char* end) : m_p(p), m_end(end) {}
            char32_t operator*() const {
                unsigned char c = static_cast<unsigned char>(*m_p);
                if (c < 0x80) return c;
                if ((c & 0xE0) == 0xC0) {
                    if (m_p + 1 >= m_end) return c;
                    return ((c & 0x1F) << 6) | (static_cast<unsigned char>(m_p[1]) & 0x3F);
                }
                if ((c & 0xF0) == 0xE0) {
                    if (m_p + 2 >= m_end) return c;
                    return ((c & 0x0F) << 12) | ((static_cast<unsigned char>(m_p[1]) & 0x3F) << 6) | (static_cast<unsigned char>(m_p[2]) & 0x3F);
                }
                if ((c & 0xF8) == 0xF0) {
                    if (m_p + 3 >= m_end) return c;
                    return ((c & 0x07) << 18) | ((static_cast<unsigned char>(m_p[1]) & 0x3F) << 12) | ((static_cast<unsigned char>(m_p[2]) & 0x3F) << 6) | (static_cast<unsigned char>(m_p[3]) & 0x3F);
                }
                return c;
            }
            const_iterator& operator++() {
                unsigned char c = static_cast<unsigned char>(*m_p);
                ptrdiff_t adv = 1;
                if ((c & 0xE0) == 0xC0) adv = 2;
                else if ((c & 0xF0) == 0xE0) adv = 3;
                else if ((c & 0xF8) == 0xF0) adv = 4;
                m_p += std::min(adv, m_end - m_p);
                return *this;
            }
            bool operator!=(const const_iterator& other) const { return m_p != other.m_p; }
            bool operator==(const const_iterator& other) const { return m_p == other.m_p; }
        private:
            const char* m_p;
            const char* m_end;
        };

        const_iterator begin() const { return { c_str(), c_str() + std::string::length() }; }
        const_iterator end()   const { return { c_str() + std::string::length(), c_str() + std::string::length() }; }

        void append_unichar(char32_t c) {
            if (c < 0x80) {
                push_back(static_cast<char>(c));
            } else if (c < 0x800) {
                push_back(static_cast<char>(0xC0 | (c >> 6)));
                push_back(static_cast<char>(0x80 | (c & 0x3F)));
            } else if (c < 0x10000) {
                push_back(static_cast<char>(0xE0 | (c >> 12)));
                push_back(static_cast<char>(0x80 | ((c >> 6) & 0x3F)));
                push_back(static_cast<char>(0x80 | (c & 0x3F)));
            } else {
                push_back(static_cast<char>(0xF0 | (c >> 18)));
                push_back(static_cast<char>(0x80 | ((c >> 12) & 0x3F)));
                push_back(static_cast<char>(0x80 | ((c >> 6) & 0x3F)));
                push_back(static_cast<char>(0x80 | (c & 0x3F)));
            }
        }

        ustring normalize(NormalizeMode mode) const {
            if (mode != NormalizeMode::ALL) return *this;
            ustring res;
            res.reserve(bytes());
            for (char32_t c : *this) {
                switch (c) {
                    case 0x00C0: res += "A\xCC\x80"; break; case 0x00C1: res += "A\xCC\x81"; break;
                    case 0x00C2: res += "A\xCC\x82"; break; case 0x00C3: res += "A\xCC\x83"; break;
                    case 0x00C4: res += "A\xCC\x88"; break; case 0x00C5: res += "A\xCC\x8A"; break;
                    case 0x00C7: res += "C\xCC\xA7"; break; case 0x00C8: res += "E\xCC\x80"; break;
                    case 0x00C9: res += "E\xCC\x81"; break; case 0x00CA: res += "E\xCC\x82"; break;
                    case 0x00CB: res += "E\xCC\x88"; break; case 0x00CC: res += "I\xCC\x80"; break;
                    case 0x00CD: res += "I\xCC\x81"; break; case 0x00CE: res += "I\xCC\x82"; break;
                    case 0x00CF: res += "I\xCC\x88"; break; case 0x00D1: res += "N\xCC\x83"; break;
                    case 0x00D2: res += "O\xCC\x80"; break; case 0x00D3: res += "O\xCC\x81"; break;
                    case 0x00D4: res += "O\xCC\x82"; break; case 0x00D5: res += "O\xCC\x83"; break;
                    case 0x00D6: res += "O\xCC\x88"; break; case 0x00D9: res += "U\xCC\x80"; break;
                    case 0x00DA: res += "U\xCC\x81"; break; case 0x00DB: res += "U\xCC\x82"; break;
                    case 0x00DC: res += "U\xCC\x88"; break; case 0x00DD: res += "Y\xCC\x81"; break;
                    case 0x00E0: res += "a\xCC\x80"; break; case 0x00E1: res += "a\xCC\x81"; break;
                    case 0x00E2: res += "a\xCC\x82"; break; case 0x00E3: res += "a\xCC\x83"; break;
                    case 0x00E4: res += "a\xCC\x88"; break; case 0x00E5: res += "a\xCC\x8A"; break;
                    case 0x00E7: res += "c\xCC\xA7"; break; case 0x00E8: res += "e\xCC\x80"; break;
                    case 0x00E9: res += "e\xCC\x81"; break; case 0x00EA: res += "e\xCC\x82"; break;
                    case 0x00EB: res += "e\xCC\x88"; break; case 0x00EC: res += "i\xCC\x80"; break;
                    case 0x00ED: res += "i\xCC\x81"; break; case 0x00EE: res += "i\xCC\x82"; break;
                    case 0x00EF: res += "i\xCC\x88"; break; case 0x00F1: res += "n\xCC\x83"; break;
                    case 0x00F2: res += "o\xCC\x80"; break; case 0x00F3: res += "o\xCC\x81"; break;
                    case 0x00F4: res += "o\xCC\x82"; break; case 0x00F5: res += "o\xCC\x83"; break;
                    case 0x00F6: res += "o\xCC\x88"; break; case 0x00F9: res += "u\xCC\x80"; break;
                    case 0x00FA: res += "u\xCC\x81"; break; case 0x00FB: res += "u\xCC\x82"; break;
                    case 0x00FC: res += "u\xCC\x88"; break; case 0x00FD: res += "y\xCC\x81"; break;
                    case 0x00FF: res += "y\xCC\x88"; break;
                    default: res.append_unichar(c); break;
                }
            }
            return res;
        }

        ustring uppercase() const {
            ustring res = *this;
            std::string& s = res;
            std::transform(s.begin(), s.end(), s.begin(), [](unsigned char c){ return std::toupper(c); });
            return res;
        }

        ustring lowercase() const {
            ustring res = *this;
            std::string& s = res;
            std::transform(s.begin(), s.end(), s.begin(), [](unsigned char c){ return std::tolower(c); });
            return res;
        }

        // Glib::ustring::compose implementation for translations (%1, %2... style)
        template<typename... Args>
        static ustring compose(const std::string& fmt, const Args&... args) {
            std::vector<std::string> arg_strs = { to_str(args)... };
            std::string res;
            res.reserve(fmt.length() * 2);
            for (size_t i = 0; i < fmt.length(); ++i) {
                if (fmt[i] == '%' && i + 1 < fmt.length()) {
                    if (fmt[i+1] == '%') {
                        res += '%';
                        i++;
                    } else if (std::isdigit(static_cast<unsigned char>(fmt[i+1]))) {
                        int arg_num = fmt[i+1] - '0';
                        size_t j = i + 2;
                        if (j < fmt.length() && std::isdigit(static_cast<unsigned char>(fmt[j]))) {
                            arg_num = arg_num * 10 + (fmt[j] - '0');
                            j++;
                        }
                        if (arg_num >= 1 && static_cast<size_t>(arg_num) <= arg_strs.size()) {
                            res += arg_strs[arg_num - 1];
                            i = j - 1;
                        } else {
                            res += '%';
                        }
                    } else {
                        res += '%';
                    }
                } else {
                    res += fmt[i];
                }
            }
            return ustring(res);
        }

    private:
        template<typename T>
        static std::string to_str(const T& val) {
            std::ostringstream oss;
            oss << val;
            return oss.str();
        }
    };

    class Error : public std::runtime_error {
    public:
        using std::runtime_error::runtime_error;
    };

    class RegexError : public Error {
    public:
        using Error::Error;
    };

    class FileError : public Error {
    public:
        using Error::Error;
    };

    template<typename T>
    class RefPtr : public std::shared_ptr<T> {
    public:
        using std::shared_ptr<T>::shared_ptr;
        RefPtr() : std::shared_ptr<T>(nullptr) {}
        RefPtr(const std::shared_ptr<T>& other) : std::shared_ptr<T>(other) {}
        operator bool() const { return( this->get() != nullptr ); }
        static RefPtr<T> create() { return RefPtr<T>(nullptr); }
        T* operator->() const { return std::shared_ptr<T>::get(); }
    };

    class Regex;

    class MatchInfo {
    public:
        MatchInfo() : m_regex(nullptr), m_pos(0) {}
        bool next();
        void fetch_pos(int num, int& bgn, int& end) const {
            end = m_pos;
            bgn = end - m_match.length();
        }

        void init(const Regex* regex, const std::string& s) {
            m_regex = regex;
            m_string = s;
            m_pos = 0;
        }

    private:
        const Regex* m_regex;
        std::string m_string;
        size_t m_pos;
        re2::StringPiece m_match;
        friend class Regex;
    };

    class Regex {
    public:
        enum class CompileFlags {
            DEFAULT = 0,
            CASELESS = 1 << 0,
            UNGREEDY = 1 << 1
        };

        static RefPtr<Regex> create(const std::string& pattern, CompileFlags flags = CompileFlags::DEFAULT) {
            return std::make_shared<Regex>(pattern, flags);
        }

        Regex(const std::string& pattern, CompileFlags flags) {
            re2::RE2::Options options;
            if (static_cast<int>(flags) & static_cast<int>(CompileFlags::CASELESS)) {
                options.set_case_sensitive(false);
            }
            m_re2 = std::make_unique<re2::RE2>(pattern, options);
            if (!m_re2->ok()) {
                throw RegexError("Invalid regex: " + pattern);
            }
        }

        bool match(const std::string& s, MatchInfo& mi) const {
            mi.init(this, s);
            // construct StringPiece starting from last match end
            re2::StringPiece input(s.data() + mi.m_pos, s.size() - mi.m_pos);

            if (re2::RE2::PartialMatch(input, *m_re2, &mi.m_match)) {
                mi.m_pos = (mi.m_match.data() - s.data()) + mi.m_match.length();
                return true;
            }
            return false;
        }

        const re2::RE2& get_re2() const { return *m_re2; }

    private:
        std::unique_ptr<re2::RE2> m_re2;
    };

    inline bool MatchInfo::next() {
        if (!m_regex || m_pos >= m_string.length()) return false;
        re2::StringPiece input(m_string.data() + m_pos, m_string.length() - m_pos);
        if (re2::RE2::PartialMatch(input, m_regex->get_re2(), &m_match)) {
            m_pos = (m_match.data() - m_string.data()) + m_match.length();
            return true;
        }
        return false;
    }

    inline Regex::CompileFlags operator|(Regex::CompileFlags a, Regex::CompileFlags b) {
        return static_cast<Regex::CompileFlags>(static_cast<int>(a) | static_cast<int>(b));
    }

    class DateTime {
    public:
        static RefPtr<DateTime> create_now_local() { return nullptr; }
        int get_year() const { return 0; }
        int get_month() const { return 0; }
        int get_day_of_month() const { return 0; }
        int get_hour() const { return 0; }
        int get_minute() const { return 0; }
        int get_second() const { return 0; }
    };

    class Date {
    public:
        enum Month { JANUARY = 1, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST, SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER, BAD_MONTH };
        Date() {}
        Date(unsigned char d, Month m, unsigned short y) {}
    };

    namespace Unicode {
        inline bool isalnum(char32_t c) { if (c > 127) return true; return std::isalnum(static_cast<unsigned char>(c)); }
        inline bool isalpha(char32_t c) { if (c > 127) return true; return std::isalpha(static_cast<unsigned char>(c)); }
        inline bool isdigit(char32_t c) { return std::isdigit(static_cast<unsigned char>(c)); }
        inline bool isupper(char32_t c) { return std::isupper(static_cast<unsigned char>(c)); }
        inline char32_t tolower(char32_t c) { return std::tolower(static_cast<unsigned char>(c)); }
        inline char32_t toupper(char32_t c) { return std::toupper(static_cast<unsigned char>(c)); }
    }

    namespace Ascii {
        inline bool isalpha(char c) { return std::isalpha(static_cast<unsigned char>(c)); }
    }

    class Dispatcher {
    public:
        std::function<void()> m_callback;

        void connect(const std::function<void()>& ptr) { m_callback = ptr; }

        void emit() {
            if (m_callback) {
                extern void trigger_android_dispatcher(Dispatcher* d);
                trigger_android_dispatcher(this);
            }
        }
    };

    namespace Markup {
        inline std::string escape_text(const std::string& s) {
            std::string res;
            res.reserve(s.size());
            for (char c : s) {
                switch (c) {
                    case '&':  res += "&amp;";  break;
                    case '<':  res += "&lt;";   break;
                    case '>':  res += "&gt;";   break;
                    case '"':  res += "&quot;"; break;
                    case '\'': res += "&apos;"; break;
                    default:   res += c;        break;
                }
            }
            return res;
        }
    }

    inline std::string get_home_dir() { return ""; }
    inline std::string getenv(const std::string& name) { return ""; }

    inline std::string uri_escape_string(const std::string& s, const std::string& reserved_chars_allowed = "", bool allow_utf8 = true) {
        std::string result;
        static const char hex[] = "0123456789ABCDEF";
        for (unsigned char c : s) {
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
                c == '-' || c == '_' || c == '.' || c == '~' ||
                (allow_utf8 && c > 127) ||
                reserved_chars_allowed.find(c) != std::string::npos) {
                result += c;
            } else {
                result += '%';
                result += hex[c >> 4];
                result += hex[c & 0xF];
            }
        }
        return result;
    }

    inline std::string filename_from_uri(const std::string& uri) {
        if (uri.size() >= 7 && uri.substr(0, 7) == "file://") {
            return uri.substr(7);
        }
        return uri;
    }

    inline std::string build_filename(const std::string& a, const std::string& b) {
        return a + "/" + b;
    }

    extern std::string get_filename_from_android(const std::string& uri);
    extern float get_chart_label_size_from_android();
}

enum GUnicodeType {
    G_UNICODE_CONTROL, G_UNICODE_FORMAT, G_UNICODE_UNASSIGNED, G_UNICODE_PRIVATE_USE,
    G_UNICODE_SURROGATE, G_UNICODE_LOWERCASE_LETTER, G_UNICODE_MODIFIER_LETTER,
    G_UNICODE_OTHER_LETTER, G_UNICODE_TITLECASE_LETTER, G_UNICODE_UPPERCASE_LETTER,
    G_UNICODE_SPACING_MARK, G_UNICODE_ENCLOSING_MARK, G_UNICODE_NON_SPACING_MARK,
    G_UNICODE_DECIMAL_NUMBER, G_UNICODE_LETTER_NUMBER, G_UNICODE_OTHER_NUMBER,
    G_UNICODE_CONNECT_PUNCTUATION, G_UNICODE_DASH_PUNCTUATION, G_UNICODE_CLOSE_PUNCTUATION,
    G_UNICODE_FINAL_PUNCTUATION, G_UNICODE_INITIAL_PUNCTUATION, G_UNICODE_OPEN_PUNCTUATION,
    G_UNICODE_OTHER_PUNCTUATION, G_UNICODE_CURRENCY_SYMBOL, G_UNICODE_MODIFIER_SYMBOL,
    G_UNICODE_MATH_SYMBOL, G_UNICODE_OTHER_SYMBOL, G_UNICODE_LINE_SEPARATOR,
    G_UNICODE_PARAGRAPH_SEPARATOR, G_UNICODE_SPACE_SEPARATOR
};

typedef char32_t gunichar;

inline GUnicodeType g_unichar_type(gunichar c) {
    if (c >= 0x0300 && c <= 0x036F) return G_UNICODE_NON_SPACING_MARK;
    if (c >= 0x1AB0 && c <= 0x1AFF) return G_UNICODE_NON_SPACING_MARK;
    if (c >= 0x1DC0 && c <= 0x1DFF) return G_UNICODE_NON_SPACING_MARK;
    if (c >= 0x20D0 && c <= 0x20FF) return G_UNICODE_NON_SPACING_MARK;
    if (c >= 0xFE20 && c <= 0xFE2F) return G_UNICODE_NON_SPACING_MARK;
    return G_UNICODE_OTHER_LETTER;
}

namespace Gdk {
    enum class InterpType { BILINEAR };

    class Pixbuf {
    public:
        int get_width() const { return 0; }
        int get_height() const { return 0; }
        static Glib::RefPtr<Pixbuf> create_from_file(const std::string&) { return nullptr; }
        Glib::RefPtr<Pixbuf> scale_simple(int, int, InterpType) { return nullptr; }
    };

    class PixbufError : public Glib::Error {
    public:
        using Glib::Error::Error;
    };

    class RGBA {
    public:
        RGBA() : r(0), g(0), b(0), a(1) {}
        RGBA(const std::string& hex) { set(hex); }
        RGBA(double r_, double g_, double b_, double a_ = 1) : r(r_), g(g_), b(b_), a(a_) {}
        void set(const std::string& hex) {
            r = g = b = 0; a = 1;
            if (hex.empty()) return;
            try {
                if (hex[0] == '#') {
                    if (hex.length() == 7) { // #RRGGBB
                        r = std::stoi(hex.substr(1, 2), nullptr, 16) / 255.0;
                        g = std::stoi(hex.substr(3, 2), nullptr, 16) / 255.0;
                        b = std::stoi(hex.substr(5, 2), nullptr, 16) / 255.0;
                    } else if (hex.length() == 9) { // #AARRGGBB
                        a = std::stoi(hex.substr(1, 2), nullptr, 16) / 255.0;
                        r = std::stoi(hex.substr(3, 2), nullptr, 16) / 255.0;
                        g = std::stoi(hex.substr(5, 2), nullptr, 16) / 255.0;
                        b = std::stoi(hex.substr(7, 2), nullptr, 16) / 255.0;
                    } else if (hex.length() == 13) { // #RRRRGGGGBBBB
                        r = std::stoi(hex.substr(1, 4), nullptr, 16) / 65535.0;
                        g = std::stoi(hex.substr(5, 4), nullptr, 16) / 65535.0;
                        b = std::stoi(hex.substr(9, 4), nullptr, 16) / 65535.0;
                    }
                }
            } catch (...) {}
        }

        void set_rgba(double r_, double g_, double b_, double a_ ) {
            r = r_;
            g = g_;
            b = b_;
            a = a_;
        }

        std::string to_string() const {
            char buf[10];
            snprintf(buf, sizeof(buf), "#%02X%02X%02X",
                (int)std::round(r * 255),
                (int)std::round(g * 255),
                (int)std::round(b * 255));
            return buf;
        }
        double get_red() const { return r; }
        double get_green() const { return g; }
        double get_blue() const { return b; }
        double get_alpha() const { return a; }
        unsigned short get_red_u() const { return (unsigned short)(r * 65535.0); }
        unsigned short get_green_u() const { return (unsigned short)(g * 65535.0); }
        unsigned short get_blue_u() const { return (unsigned short)(b * 65535.0); }

        void set_red_u(double val) { r = val / 65535.0; }
        void set_green_u(double val) { g = val / 65535.0; }
        void set_blue_u(double val) { b = val / 65535.0; }
        void set_alpha(double val) { a = val; }
    private:
        double r, g, b, a;
    };

    enum class ModifierType { NONE = 0 };
}

namespace Gtk {
    class IconPaintable {};
}

namespace Gio {
    inline void init() {}

    class Error : public Glib::Error {
    public:
        using Glib::Error::Error;
    };

    enum class FileType { DIRECTORY, REGULAR, SYMBOLIC_LINK };
    enum class FileQueryInfoFlags { NONE = 0 };

    class FileInfo {
    public:
        bool get_attribute_boolean(const std::string&) const { return true; }
        FileType get_file_type() const { return FileType::REGULAR; }
        std::string get_name() const { return ""; }
        std::string get_symlink_target() const { return ""; }
        gsize get_size() const { return 0; }
    };

    class FileEnumerator {
    public:
        Glib::RefPtr<FileInfo> next_file() { return nullptr; }
    };

//    class FileInputStream {
//    public:
//        bool read_all(void* buffer, gsize count, gsize& bytes_read) { return false; }
//        void close() {}
//    };
//
//    class FileOutputStream {
//    public:
//        void write_all(const std::string& s, gsize& bytes_written) { bytes_written = s.size(); }
//        void write_all(const void* buffer, gsize count, gsize& bytes_written) { bytes_written = count; }
//        bool write_all(const void* buffer, gsize count, gsize& bytes_written, void* cancellable) { bytes_written = count; return true; }
//        void close() {}
//    };

    class File {
    public:
        File(const std::string& s = "") : m_s(s) {}
        enum class CopyFlags { NONE = 0, OVERWRITE = 1 << 0 };

        static Glib::RefPtr<File> create_for_uri(const std::string& uri) { return std::make_shared<File>(uri); }
        static Glib::RefPtr<File> create_for_commandline_arg(const std::string& arg) { return std::make_shared<File>(arg); }
        static Glib::RefPtr<File> create_for_path(const std::string& path) { return std::make_shared<File>("file://" + path); }

        std::string get_uri() const { return m_s; }
        std::string get_path() const {
            if (m_s.size() >= 7 && m_s.substr(0, 7) == "file://") return m_s.substr(7);
            return m_s;
        }
        std::string get_basename() const {
            if (m_s.compare(0, 10, "content://") == 0) {
                return Glib::get_filename_from_android(m_s);
            }
            size_t pos = m_s.find_last_of('/');
            if (pos == std::string::npos) return m_s;
            return m_s.substr(pos + 1);
        }
        bool query_exists() const { return true; }

        Glib::RefPtr<FileInfo> query_info(const std::string& attributes = "", FileQueryInfoFlags flags = FileQueryInfoFlags::NONE) { return std::make_shared<FileInfo>(); }
        Glib::RefPtr<FileEnumerator> enumerate_children() { return std::make_shared<FileEnumerator>(); }

//        Glib::RefPtr<FileInputStream> read() { return std::make_shared<FileInputStream>(); }
//        Glib::RefPtr<FileOutputStream> create_file() { return std::make_shared<FileOutputStream>(); }
//        Glib::RefPtr<FileOutputStream> replace() { return std::make_shared<FileOutputStream>(); }

        Glib::RefPtr<File> get_parent() const {
            size_t pos = m_s.find_last_of('/');
            if (pos == std::string::npos || pos == 0) return nullptr;
            return std::make_shared<File>(m_s.substr(0, pos));
        }
        Glib::RefPtr<File> get_child(const std::string& name) const { return std::make_shared<File>(m_s + "/" + name); }
    private:
        std::string m_s;
    };

    class SimpleActionGroup {};
    class SimpleAction {};
}

#define G_FILE_ATTRIBUTE_ACCESS_CAN_WRITE "access::can-write"
#define G_FILE_ATTRIBUTE_ACCESS_CAN_READ "access::can-read"

namespace sigc {
    template<typename T> class signal;

    template<typename R, typename... Args>
    class signal<R(Args...)> {
    public:
        void connect(const std::function<R(Args...)>& slot) { m_slots.push_back(slot); }
        void emit(Args... args) { for(auto& slot : m_slots) slot(args...); }
    private:
        std::vector<std::function<R(Args...)>> m_slots;
    };

    template<typename T, typename M>
    inline std::function<void()> mem_fun(T& obj, M mem_ptr) {
        return [ptr = &obj, mem_ptr]() { (ptr->*mem_ptr)(); };
    }
    template<typename T, typename M>
    inline std::function<void()> mem_fun(T* obj, M mem_ptr) {
        return [obj, mem_ptr]() { (obj->*mem_ptr)(); };
    }
}

namespace Pango {
    enum FontMask {
        NONE = 0,
        FAMILY = 1
    };

    class FontDescription {
    public:
        FontDescription() : m_size( 0 ) {}

        FontDescription(const std::string& description) : m_size( 0 ) {
            size_t last_space = description.find_last_of(' ');
            if (last_space != std::string::npos) {
                try {
                    m_size = std::stoi(description.substr(last_space + 1));
                    m_family = description.substr(0, last_space);
                } catch (...) {
                    m_family = description;
                }
            } else {
                m_family = description;
            }
        }

        [[nodiscard]] std::string to_string() const {
            return m_size > 0 ? m_family + " " + std::to_string(m_size) : m_family;
        }

        static bool get_size_is_absolute() { return true; }

        [[nodiscard]] int get_size() const { return m_size; }

        [[nodiscard]] std::string get_family() const { return m_family; }

        FontMask get_set_fields() const { return m_family.empty() ? NONE: FAMILY; }

    private:
        std::string     m_family;
        int             m_size;
    };

    class FontMetrics {
    };

    enum class Alignment {
        LEFT, CENTER, RIGHT
    };
}

// Enchant stubs
typedef struct _EnchantBroker EnchantBroker;
typedef struct _EnchantDict EnchantDict;

typedef void (*EnchantDictDescribeCallback) (const char * const lang_tag,
                                             const char * const provider_name,
                                             const char * const provider_desc,
                                             const char * const provider_file,
                                             void * user_data);

inline EnchantDict* enchant_broker_request_dict(EnchantBroker* broker, const char* lang) { return nullptr; }
inline void enchant_broker_free_dict(EnchantBroker* broker, EnchantDict* dict) {}
inline int enchant_dict_check(EnchantDict* dict, const char* word, size_t len) { return 0; }
inline void enchant_dict_describe(EnchantDict* dict, EnchantDictDescribeCallback cb, void* user_data) {}

#endif // LIFEOGRAPH_ANDROID_SHIM_HPP
