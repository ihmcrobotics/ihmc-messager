plugins {
   id("us.ihmc.ihmc-build")
   id("us.ihmc.ihmc-ci") version "7.4"
   id("us.ihmc.ihmc-cd") version "1.20"
}

ihmc {
   group = "us.ihmc"
   version = "0.1.8"
   vcsUrl = "https://github.com/ihmcrobotics/ihmc-messager"
   openSource = true

   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   api("net.sf.trove4j:trove4j:3.0.3")
   api("us.ihmc:ihmc-commons:0.30.4")
   api("us.ihmc:log-tools:0.6.1")
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
   api("us.ihmc:ihmc-commons-testing:0.30.4")
}
