
## [v0.0.12](https://github.com/ffalt/starfield/compare/v0.0.12) (2026-04-06)

### Features

 -  **app**  better icon ([756a18b3d4a1a9a](https://github.com/ffalt/starfield/commit/756a18b3d4a1a9ad31fe5e14bb907cd126a3ba85))
 -  **starfield**  new nebula clouds effect ([910ec9f2c941e54](https://github.com/ffalt/starfield/commit/910ec9f2c941e54a71ac903ac17c641007a94b73))
 -  **starfield**  configurable background color and optional nebula gradient ([660b554e49a0cd9](https://github.com/ffalt/starfield/commit/660b554e49a0cd9002d65fec1681cf88d58aa0e1))


## [v0.0.11](https://github.com/ffalt/starfield/compare/v0.0.11) (2026-03-16)

### Features

 -  **starfield**  improve null safety and size initialization ([3634e18815ccfb7](https://github.com/ffalt/starfield/commit/3634e18815ccfb7e0b0484a9cdc3a5e1ed5b1978))
 -  **settings**  add follow screen intensity ([2a7cbe7d5c5698a](https://github.com/ffalt/starfield/commit/2a7cbe7d5c5698a009440af2a1a4b578ccef1993))

### Bug Fixes

 -  **StarfieldOpts**  implement double-checked locking for SharedPreferences initialization ([a6f29cfb8818077](https://github.com/ffalt/starfield/commit/a6f29cfb881807796c3a047a42611d2e8e035aba))
 -  **starfield**  change stars_current_brightness from float to int for improved performance ([596bdc70ef819be](https://github.com/ffalt/starfield/commit/596bdc70ef819be8853a03e3c5913e89178d301d))
 -  **starfield**  replace System.currentTimeMillis() with SystemClock.elapsedRealtime() for accurate frame timing ([1616ff315da35db](https://github.com/ffalt/starfield/commit/1616ff315da35db7eb68e010f8726684db9f38f1))
 -  **starfield**  optimize tilt handling in sensor changes for better performance ([0272308e4d02e27](https://github.com/ffalt/starfield/commit/0272308e4d02e27a3a68edf23079908061f40f78))
 -  **MainActivity**  add error message for wallpaper picker not found ([896f6a664b0a99a](https://github.com/ffalt/starfield/commit/896f6a664b0a99a099e14ec9c9bbb81b9bce22ea))
 -  **starfield**  add null checks for preference change listeners ([a8199a13e33f221](https://github.com/ffalt/starfield/commit/a8199a13e33f22108c43b3482fc867fc4d9eeac9))
 -  **starfield**  improve drawTime calculation for better precision ([788b93e0296a9c7](https://github.com/ffalt/starfield/commit/788b93e0296a9c72bfe192163a2e87e3ced45fd2))
 -  **startrail**  replace setColor with setAlpha to restore trail brightness ([e5642445f472326](https://github.com/ffalt/starfield/commit/e5642445f4723262baa3d1a4361bfd518dd7b3a1))
 -  **meteors**  correct speedBase calculation for meteor spawning ([61f127b4d5a6e4c](https://github.com/ffalt/starfield/commit/61f127b4d5a6e4c099ef62376e0d45fa0e52a8ea))
 -  **sensor**  fix race condition by posting sensor updates to main thread ([04fee2f2a8b0908](https://github.com/ffalt/starfield/commit/04fee2f2a8b09084ace63d5d323584f61c449580))

## [v0.0.10](https://github.com/ffalt/starfield/compare/v0.0.10) (2025-12-13)

### Features

 -  **swiping**  smooth star movement on homescreen changing ([24250d9af374d35](https://github.com/ffalt/starfield/commit/24250d9af374d3543e8b80053de4c2344470a5dd))

## [v0.0.9](https://github.com/ffalt/starfield/compare/v0.0.9) (2025-12-12)

 - **translation** add spanish translation, thanks to https://crowdin.com/profile/gjostin769

## [v0.0.8](https://github.com/ffalt/starfield/compare/v0.0.8) (2025-12-12)

### Features

 -  **space**  option for rare meteors ([677c4d78ca5fec9](https://github.com/ffalt/starfield/commit/677c4d78ca5fec9daa6b9984f0fa5c677f90fe31))
 -  **sensors**  option to adjust speed to battery level ([3bfd772f19afed2](https://github.com/ffalt/starfield/commit/3bfd772f19afed2d96f337a0b111ef27c509e3a7))


## [v0.0.7](https://github.com/ffalt/starfield/compare/v0.0.7) (2025-12-07)

### Bug Fixes

 -  **settings**  ensure window is not behind status bar ([069d8daf7f94f22](https://github.com/ffalt/starfield/commit/069d8daf7f94f22ce38cf40acbca8876bcc889a9))

## [v0.0.6](https://github.com/ffalt/starfield/compare/v0.0.6) (2025-12-07)

- **sensor**: tilt stars based on device gyroscope

## [v0.0.5](https://github.com/ffalt/starfield/compare/v0.0.5) (2025-10-29)

### Features

-  **dependencies** update

## [v0.0.4](https://github.com/ffalt/starfield/compare/v0.0.4) (2024-04-03)

### Features

-  **translations**  add Russian thanks to https://crowdin.com/profile/ronner231 ([20d601262163a3e](https://github.com/ffalt/starfield/commit/20d601262163a3e539c472f3cb61e73937bd1ee1))

## [v0.0.3](https://github.com/ffalt/starfield/compare/v0.0.3) (2024-04-02)

### Features

 -  **build**  automatic version code ([804c2e70d88074e](https://github.com/ffalt/starfield/commit/804c2e70d88074e8bf1bbaa84d0df75bc083fadc))
 -  **build**  add build to version code ([76488d5700c6123](https://github.com/ffalt/starfield/commit/76488d5700c61238e5011045a3f32b9ff6ae4584))
 -  **build**  add bump version task ([46e464647d42882](https://github.com/ffalt/starfield/commit/46e464647d42882bae51151211837fc3504b7638))
 -  **build**  add bump version task ([ca1a9cde98a5b32](https://github.com/ffalt/starfield/commit/ca1a9cde98a5b32db71b8c3d4d5f272c1c08e447))
 -  **build**  add bump version task ([8d8818612973a84](https://github.com/ffalt/starfield/commit/8d8818612973a8410b58133be74761d9981fdcf0))
 -  **build**  add bump version task ([cdb0d5a8835d639](https://github.com/ffalt/starfield/commit/cdb0d5a8835d639ca7584562ce33585b83f9bea4))
 -  **build**  release task ([0bd75d19ee7e476](https://github.com/ffalt/starfield/commit/0bd75d19ee7e4768e73d0e127a8894d5bcea651d))

### Bug Fixes

 -  (main activity): version number display ([b5504062f31a244](https://github.com/ffalt/starfield/commit/b5504062f31a244aed9d556a8d877612db1b1a85))
 -  **build**  sign release commit ([b5e1cf50cff7959](https://github.com/ffalt/starfield/commit/b5e1cf50cff7959bf43b8f27652f57c7838ae8dc))

## [v0.0.2](https://github.com/ffalt/starfield/compare/v0.0.2) (2024-04-01)

### Features

 -  **build**  use versions from git tags ([05602a606efcb75](https://github.com/ffalt/starfield/commit/05602a606efcb75695abd369e0986793b9e1e984))
 -  prepare release notes via action ([1d43f65717bd0a5](https://github.com/ffalt/starfield/commit/1d43f65717bd0a5589f14cd040a7331c20f90e79))
 -  prepare release notes via action ([bff5e9d0204d5a6](https://github.com/ffalt/starfield/commit/bff5e9d0204d5a64befece186caf7387b63712df))
 -  prepare release via action ([dbaafec1f6b91eb](https://github.com/ffalt/starfield/commit/dbaafec1f6b91eb38fb011142aa6a6643068e2db))
 -  prepare release ([16239ac69c66a8c](https://github.com/ffalt/starfield/commit/16239ac69c66a8c4a367d37652106e43d8554514))

### Bug Fixes

 -  must fetch full repository to generate release notes ([3ee152dbaa3826b](https://github.com/ffalt/starfield/commit/3ee152dbaa3826b8d64663a0fe861ff5d320a44b))

## [v0.0.1](https://github.com/ffalt/starfield/compare/v0.0.1) (2024-03-27)


 
