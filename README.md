# Protean Beers

A contrived sample API and webapp built with Clojure.  Figures out ingredients for different beverages and lets you brew them :-)

The idea is to illustrate some ideas in building a 'regular' API, and the benefits this can yield.  The webapp is not as simple
as it could be as it employs some componentisation (decoupling) etc.

Will eventually be loosely based on [Wikipedia Brewing](http://en.wikipedia.org/wiki/Brewing).

Includes a [Protean](https://github.com/passivsystems/protean) codex *beers.edn* demonstrating how to simulate and automatically integration test with Protean.


## Usage

### Starting the service

    lein deps
    lein run

 runs on port 3002

### Accessing resources

List starches:

    curl -v -H 'Authorization: XYZBearer token' 'http://host:3002/beers/starches'

List yeasts:

    curl -v -H 'Authorization: XYZBearer token' 'http://host:3002/beers/yeasts'

List flavourings:

	curl -v -H 'Authorization: XYZBearer token' 'http://host:3002/beers/flavourings'

Get a starch source for ale beverage type:

    curl -v -H 'Authorization: XYZBearer token' 'http://host:3002/beers/starches/pick?drink=ale'

Get a yeast for lager beverage type:

    curl -v -H 'Authorization: XYZBearer token' 'http://host:3002/beers/yeasts/pick?drink=lager'

Brew a beverage:

    curl -v -X POST -H 'Authorization: XYZBearer token' -H 'Content-Type: application/json' -H 'Content-type: application/json' --data '{"starch":"/starches/wheat","yeast":"/yeasts/yeast","flavouring":"/flavourings/golding-hops"}' 'http://host:3002/beers/brew'


### Automatically integration testing the service

The below will automatically (incrementally) integration test the service.  Host and port should be configured to point to an instance of Protean (admin port).

Testing specific resources:

    curl -v -X POST -H "Content-Type: application/json" --data '{"port":3002,"locs":["beers token starches/pick yeasts/pick flavourings/pick"],"seed":{"drink":"ale"}}' 'http://host:port/test'

Testing the entire service:

    curl -v -X POST -H "Content-Type: application/json" --data '{"port":3002,"locs":["beers"],"seed":{"drink":"ale"}}' 'http://localhost:3001/test'


## Contributing

All contributions ideas/pull requests/bug reports are welcome, we hope you find it useful. 


## License

Protean is licensed with Apache License v2.0.