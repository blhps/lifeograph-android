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

typedef size_t gsize;

#define PANGO_SCALE 1024

// Minimal Glib/Gtk stubs for Android build
namespace Glib {
    class ustring : public std::string {
    public:
        using std::string::string;
        ustring(const std::string& s) : std::string(s) {}
        ustring(const char* s) : std::string(s ? s : "") {}
        ustring() = default;

        size_type length() const {
            size_type count = 0;
            for( unsigned char c : *this )
            {
                if( ( c & 0b11000000 ) != 0b10000000 )
                    ++count; // Only count leading bytes (start of new character)
            }
            return count;
        }

        size_type size() const { return length(); }

        size_type bytes() const { return std::string::length(); }

        ustring uppercase() const {
            ustring res = *this;
            std::transform(res.begin(), res.end(), res.begin(), [](unsigned char c){ return std::toupper(c); });
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
        static std::string to_str(const std::string& s) { return s; }
        static std::string to_str(const char* s) { return s ? s : ""; }
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
            bgn = m_match.data() - m_string.data();
            end = bgn + m_match.length();
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
            re2::StringPiece input(s);
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
        inline bool isalnum(char32_t c) { return std::isalnum(static_cast<unsigned char>(c)); }
        inline bool isalpha(char32_t c) { return std::isalpha(static_cast<unsigned char>(c)); }
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
        inline std::string escape_text(const std::string& s) { return s; }
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
}

typedef char32_t gunichar;

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

    class FileInputStream {
    public:
        bool read_all(void* buffer, gsize count, gsize& bytes_read) { return false; }
        void close() {}
    };

    class FileOutputStream {
    public:
        void write_all(const std::string& s, gsize& bytes_written) { bytes_written = s.size(); }
        void write_all(const void* buffer, gsize count, gsize& bytes_written) { bytes_written = count; }
        bool write_all(const void* buffer, gsize count, gsize& bytes_written, void* cancellable) { bytes_written = count; return true; }
        void close() {}
    };

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
            size_t pos = m_s.find_last_of('/');
            if (pos == std::string::npos) return m_s;
            return m_s.substr(pos + 1);
        }
        bool query_exists() const { return true; }
        FileType query_file_type() const { return FileType::REGULAR; }
        bool copy(const Glib::RefPtr<File>& destination, CopyFlags flags = CopyFlags::NONE) { return false; }
        bool move(const Glib::RefPtr<File>& destination, CopyFlags flags = CopyFlags::NONE) { return false; }
        bool remove() { return false; }

        Glib::RefPtr<FileInfo> query_info(const std::string& attributes = "", FileQueryInfoFlags flags = FileQueryInfoFlags::NONE) { return std::make_shared<FileInfo>(); }
        Glib::RefPtr<FileEnumerator> enumerate_children() { return std::make_shared<FileEnumerator>(); }

        Glib::RefPtr<FileInputStream> read() { return std::make_shared<FileInputStream>(); }
        Glib::RefPtr<FileOutputStream> create_file() { return std::make_shared<FileOutputStream>(); }
        Glib::RefPtr<FileOutputStream> replace() { return std::make_shared<FileOutputStream>(); }

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
        FAMILY = 1
    };

    class FontDescription {
    public:
        FontDescription() {}

        FontDescription(const std::string &) {}

        std::string to_string() const { return ""; }

        bool get_size_is_absolute() const { return true; }

        int get_size() const { return 0; }

        std::string get_family() const { return "sans"; }

        FontMask get_set_fields() const { return FAMILY; }
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
