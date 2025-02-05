plugins {
   id("us.ihmc.ihmc-build")
}

ihmc {
   group = "us.ihmc"
   version = "0.2.2"
   vcsUrl = "https://github.com/ihmcrobotics/ihmc-messager"
   openSource = true

   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   api("net.sf.trove4j:trove4j:3.0.3")
   api("us.ihmc:ihmc-commons:0.35.1")
   api("us.ihmc:log-tools:0.6.5")
}

javafxDependencies {
   api(ihmc.sourceSetProject("main"))
   var javaFXVersion = "17.0.8"
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
   api("us.ihmc:ihmc-commons-testing:0.35.1")
}
