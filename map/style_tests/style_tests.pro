TARGET = style_tests
TEMPLATE = app
CONFIG += console warn_on

!map_designer {
  CONFIG   -= app_bundle
}

INCLUDEPATH += ../../3party/protobuf/src

ROOT_DIR = ../..
DEPENDENCIES = map indexer platform geometry coding base expat protobuf

macx-*: LIBS *= "-framework IOKit"

include($$ROOT_DIR/common.pri)

QT *= core

SOURCES += \
  ../../testing/testingmain.cpp \
  classificator_tests.cpp \
  style_symbols_consistency_test.cpp \
