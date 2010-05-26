#!/bin/sh

srcdir=`dirname $0`
test -z "$srcdir" && srcdir=.

builddir=`pwd`
cd $srcdir

autoreconf -v --install || exit $?
cd $builddir || exit $?
$srcdir/configure --enable-maintainer-mode "$@"
