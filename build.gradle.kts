plugins {
   id("us.ihmc.ihmc-build")
   id("us.ihmc.ihmc-ci") version "7.4"
   id("us.ihmc.ihmc-cd") version "1.17"
}

ihmc {
   group = "us.ihmc"
   version = "0.1.6"
   vcsUrl = "https://github.com/ihmcrobotics/ihmc-messager"
   openSource = true

   configureDependencyResolution()
   configurePublications()
}

mainDependencies {
   api("net.sf.trove4j:trove4j:3.0.3")
   api("us.ihmc:ihmc-commons:0.30.4")
}

kryoDependencies {
   api(ihmc.sourceSetProject("main"))
   api("com.esotericsoftware:kryonet:2.22.0-RC1")
}

examplesDependencies {
   api(ihmc.sourceSetProject("main"))
}

testDependencies {
   api(ihmc.sourceSetProject("kryo"))
   api(ihmc.sourceSetProject("examples"))
   api("us.ihmc:ihmc-commons-testing:0.30.4")
}
