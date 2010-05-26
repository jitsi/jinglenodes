AC_INIT([ejabberg_mod_jn], [0.0.0], [johann.prieur@gmail.com])
AC_CONFIG_AUX_DIR([autotools/build-aux])
AC_CONFIG_MACRO_DIR([autotools])
AC_CONFIG_SRCDIR([Makefile.am])

AM_INIT_AUTOMAKE([foreign silent-rules dist-bzip2])
AM_MAINTAINER_MODE

AC_ERLANG_NEED_ERLC
AC_ERLANG_NEED_ERL

ERLCFLAGS="$ERLCFLAGS -I $prefix/lib/ejabberd/include -pz $prefix/lib/ejabberd/ebin"
ERLFLAGS="$ERLFLAGS -pz $prefix/lib/ejabberd/ebin"

ERL_CHECK_HEADER(ejabberd.hrl, [],
                 AC_MSG_ERROR([ejabberd.hrl is required. Make sure PREFIX is the same as the PREFIX at which ejabberd is installed.]))

ERL_CHECK_HEADER(jlib.hrl, [],
                 AC_MSG_ERROR([jlib.hrl is required. Make sure PREFIX is the same as the PREFIX at which ejabberd is installed.]))

ERL_CHECK_MODULE(gen_mod, [],
                 AC_MSG_ERROR([gen_mod module is required. Make sure PREFIX is the same as the PREFIX at which ejabberd is installed.]))

AC_SUBST(EFLAGS)

AC_ARG_ENABLE(hipe,
              AS_HELP_STRING([--enable-hipe], [compile natively with HiPE, not recommended (default: no)]),
              enable_hipe=$enableval, enable_hipe=no)
AM_CONDITIONAL(ENABLE_HIPE, test x$enable_hipe = xyes)

AC_CONFIG_FILES([Makefile
	src/Makefile])

AC_OUTPUT
