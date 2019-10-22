# KaolDB
## Description
KaolDB is a framework that offers an ORM service for the Android OS. It also offers the features of entity inheritance and polymorphic queries, which are not offered by any similiar tools at the moment of this project creation. These features are implemented only by more server related frameworks such as Hibernate, which anyway would represent an overkill for an Android device.

## Installation
It is sufficient to add the two following Gradle dependencies to the application module:

```gradle
dependencies {
    implementation 'it.mscuttari.kaoldb:core:1.0.0-beta.6'
    annotationProcessor 'it.mscuttari.kaoldb:annotation-processor:1.0.0-beta.6'
}
```

## Usage
See the [Wiki](https://github.com/mscuttari/KaolDB/wiki) for a detailed usage description.

## Contributing
Pull requests are welcome and encouraged. The only requirement is to comment the code as clearly as possible and describe the changes in the pull request by providing a concise title and a detailed body message.

## Credits
* The core module uses [JavaPoet](https://github.com/square/javapoet) to create the properties to be used during the queries.
* The core module uses [Objenesis](https://github.com/easymock/objenesis) to instantiate the query result entities.
* The test module uses [Robolectric](https://github.com/robolectric/robolectric) to run the tests in a virtual Android environment.

## License
The project is distributed under the Apache 2.0 license.