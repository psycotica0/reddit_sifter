#!/bin/sh

adb shell input keyevent 82
adb shell am start -n ca.psycoti.reddit/.HelloActivity
