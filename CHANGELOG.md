# Changelog

## 2.6.1

- Many dependency updates.
- proguard-base to 7.4.1
- proguard-core from to 9.1.1

## Some release before 2.6.1

- Allow to preserve the manifest file ([211][])
- Minimum Java version bumped to 1.8  ([127][])
- Support workaround for long command line problems ([113][])

[211]: https://github.com/wvengen/proguard-maven-plugin/issues/211
[127]: https://github.com/wvengen/proguard-maven-plugin/pull/127
[113]: https://github.com/wvengen/proguard-maven-plugin/pull/113

## 2.3.1 (released 2020-08-30)

- Respect filter on library merge ([114][])
- NoClassDefFoundError: proguard.classfile.visitor.ClassVisitor on OpenJ9 JDK 8 ([112][])

[114]: https://github.com/wvengen/proguard-maven-plugin/pull/114
[112]: https://github.com/wvengen/proguard-maven-plugin/pull/112

## 2.3.0 (released 2020-08-07)

- Support OpenJDK14 ([111][])
- Use Proguard 7.0.0 by default ([111][])

[111]: https://github.com/wvengen/proguard-maven-plugin/pull/111

## 2.2.0 (released 2019-1022)

- Use Proguard 6.1.1 by default ([64][])
- Add parameter inLibsFilter ([103][])
- Change license.txt to Apache 2.0 ([28][])
- Fix assembly and exclusions hierarchy in examples ([74][])

[64]: https://github.com/wvengen/proguard-maven-plugin/pull/64
[103]: https://github.com/wvengen/proguard-maven-plugin/pull/103
[28]: https://github.com/wvengen/proguard-maven-plugin/pull/28
[74]: https://github.com/wvengen/proguard-maven-plugin/pull/74

## 2.1.1 (released 2019-07-04)

- Fix wildcard support ([68][])
- Fix putLibraryJarsInTempDir = true ([93][])

[68]: https://github.com/wvengen/proguard-maven-plugin/pull/68
[93]: https://github.com/wvengen/proguard-maven-plugin/pull/93

## 2.1.0 (released 2019-06-08)
- Update minimum Java version to 1.6.
- Switch to proguard 6.0.3 as the default version. ([78][])
- Add support for incremental obfuscation ([73][])

[73]: https://github.com/wvengen/proguard-maven-plugin/pull/73
[78]: https://github.com/wvengen/proguard-maven-plugin/pull/78

## Older versions
- Check the commit history for details.
