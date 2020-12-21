The run time class library for QCC-based images.

This repo contains a sub-module of openjdk so clone with:
```
git clone --recurse-submodules git@github.com:quarkuscc/qcc-class-library.git
```

You must clone this repository on a case-sensitive file system.
If you are on MacOS, the default volume is case-insensitive.
You can use Disk Utility to create a case-sensitive volume, clone
the repo there, and then symlink it to be a sibling of your `qcc` clone.

OSX steps to create case-sensitive file system:
* open Disk Utility
* `File > New Image > Blank Image...`
* Pick location, and name
* Use 30 gb as a good default size
* Format: `Mac OS Extended (Case-sensitive, Journaled)`
* Image: spares disk image
* Leave other settings as default

After creating the image, unmount in Disk Utility
Mount it where you want it placed, in this case `pwd/development` using:
```
$ hdiutil attach -mountpoint `pwd`/development development.sparseimage
```
and then symlink it beside `qcc`:
```
$ ln -s <source> <dest>
```
