plugins {
   id("us.ihmc.ihmc-build")
   id("us.ihmc.ihmc-ci") version "7.7"
   id("us.ihmc.ihmc-cd") version "1.23"
}

ihmc {
   group = "us.ihmc"
   version = "0.1.9"
   vcsUrl = "https://github.com/ihmcrobotics/ihmc-messager"
   openSource = true

   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   api("net.sf.trove4j:trove4j:3.0.3")
   api("us.ihmc:ihmc-commons:0.32.0")
   api("us.ihmc:log-tools:0.6.3")
}

javafxDependencies {
   api(ihmc.sourceSetProject("main"))
   var javaFXVersion = "17.0.2"
   api(ihmc.javaFXModule("base", javaFXVersion))
   api(ihmc.javaFXModule("graphics", javaFXVersion))
}

kryoDependencies {
   api(ihmc.sourceSetProject("main"))
   api("com.github.crykn:kryonet:2.22.7") // from jitpack
}

examplesDependencies {
   api(ihmc.sourceSetProject("main"))
}

testDependencies {
   api(ihmc.sourceSetProject("kryo"))
   api(ihmc.sourceSetProject("examples"))
   api("us.ihmc:ihmc-commons-testing:0.32.0")
}
