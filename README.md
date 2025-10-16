# thneed

Clojure functions that I've found useful.

## Including

### deps.edn

    me.fogus/thneed {:mvn/version "1.1.2"}

OR

    io.github.fogus/thneed {:git/tag "v1.1.2" :git/sha "..."}

### Leiningen

Modify your [Leiningen](http://github.com/technomancy/leiningen) dependencies to include:

    :dependencies [[me.fogus/thneed "1.1.2"] ...]

### Maven

Add the following to your `pom.xml` file:

    <dependency>
      <groupId>me.fogus</groupId>
      <artifactId>thneed</artifactId>
      <version>1.1.2</version>
    </dependency>

## Dev

Namespaces under the wip sub-ns are works in progress and should only be used for experimentation. It is expected that these implementations will change frequently and may disappear altogether.

    clj -X:dev:test

To generate the current API docs run the following:

    clj -Tquickdoc quickdoc '{:outfile "doc/API.md", :github/repo "https://github.com/fogus/thneed", :git/branch "master", :toc false}'

The above requires that you install quickdocs as a CLI tool first.

## License

Copyright © 2012-2025 Fogus

Distributed under the Eclipse Public License version 2.0
