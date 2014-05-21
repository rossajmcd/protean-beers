(ns beers.core
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [compojure.core :refer [routes ANY DELETE GET POST PUT]]
            [compojure.handler :as hdr]
            [cheshire.core :as jsn])
  (:use ring.adapter.jetty)
  (:gen-class))

;; =============================================================================
;; Store
;; =============================================================================

(defn connect [host port]
  (println ";; Connecting to store")
  (reify java.io.Closeable
    (close [_] (println ";; Closing store connection"))))

(defrecord Store [host port connection]
  component/Lifecycle

  (start [component]
    (println ";; Starting Store")
    (let [conn (connect host port)]
      (assoc component :connection conn)))

  (stop [component]
    (println ";; Stopping Store")
    (.close connection)
    component))

(defn store [host port] (map->Store {:host host :port port}))

(defonce ale-starch "/starches/wheat")
(defonce ale-yeast "/yeasts/yeast")
(defonce ale-flavour "/flavourings/golding-hops")

(defonce lager-starch "/starches/lager-starch")
(defonce lager-yeast "/yeasts/lager-yeast")
(defonce lager-flavour "/flavourings/cascade-hops")

(defonce starches-src {:ale ale-starch :lager lager-starch})

(defonce yeasts-src {:ale ale-yeast :lager lager-yeast})

(defonce flavourings-src
  {:ale "/flavourings/golding-hops" :lager "/flavourings/cascade-hops"})

(defonce drinks-src
  {:ale "a crisp tasty session ale" :lager "a cool refreshing lager"})

(defn store-starches [store] (vals starches-src))

(defn store-starch [store drink]
  (println ";; connection : " (:connection store)) (drink starches-src))

(defn store-yeasts [store] (vals yeasts-src))

(defn store-yeast [store drink]
  (println ";; connection : " (:connection store)) (drink yeasts-src)) 

(defn store-flavourings [store] (vals flavourings-src))

(defn store-flavouring [store drink]
  (println ";; connection : " (:connection store)) (drink flavourings-src))

(defn store-brew [store drink]
  (println ";; connection : " (:connection store)) (drink drinks-src))


;; =============================================================================
;; Service (API)
;; =============================================================================

(defrecord Service [options store]
  component/Lifecycle

  (start [this] (println ";; Starting Service") (assoc this :store store))

  (stop [this] (println ";; Stopping Service") this))

(defn service [cfg-options] (map->Service {:options cfg-options}))

(defn token [{:keys [store]}] {"access_token" "mytoken" "token_type" "bearer"})

(defn starches [{:keys [store]}] (store-starches store))
(defn starches-pick [{:keys [store]} drink] (store-starch store drink))

(defn yeasts [{:keys [store]}] (store-yeasts store))
(defn yeasts-pick [{:keys [store]} drink] (store-yeast store drink))

(defn flavourings [{:keys [store]}] (store-flavourings store))
(defn flavourings-pick [{:keys [store]} drink] (store-flavouring store drink))

(defn brew [{:keys [store]} body] 
  (let [s (body "starch")
        y (body "yeast")
        f (body "flavouring")]
    (cond
      (and (= s ale-starch) (= y ale-yeast) (= f ale-flavour))
        {:drink "ale" :description (store-brew store :ale)}
      (and (= s lager-starch) (= y lager-yeast) (= f lager-flavour))
        {:drink "lager" :description (store-brew store :lager)}
      :else {:drink "unknown" :description "you spawned a monstrosity eugh"})))


;; =============================================================================
;; Server
;; =============================================================================

(defn default-rsp [body]
  (let [rsp {:headers {"Content-Type" "application/json"}}
        s (if body 200 404)
        rs (assoc rsp :status s)]
    (if body (assoc rs :body (jsn/generate-string body)) rs)))

(defn token-rsp [service] (default-rsp (token service)))

(defn starches-rsp [service] (default-rsp (starches service)))
(defn starches-pick-rsp [service {:keys [params]}]
  (default-rsp (starches-pick service (keyword (:drink params)))))

(defn yeasts-rsp [service] (default-rsp (yeasts service)))
(defn yeasts-pick-rsp [service {:keys [params]}]
  (default-rsp (yeasts-pick service (keyword (:drink params)))))

(defn flavours-rsp [service] (default-rsp (flavourings service)))
(defn flavours-pick-rsp [service {:keys [params]}]
  (default-rsp (flavourings-pick service (keyword (:drink params)))))

(defn brew-rsp [service req]
  (default-rsp (brew service (jsn/parse-string (slurp (:body req))))))

(defn myroutes [service]
  (routes
    (GET  "/beers/token" [] (token-rsp service))
    (GET  "/beers/starches" [] (starches-rsp service))
    (GET  "/beers/starches/pick" req (starches-pick-rsp service req))
    (GET  "/beers/yeasts" [] (yeasts-rsp service))
    (GET  "/beers/yeasts/pick" request (yeasts-pick-rsp service request))
    (GET  "/beers/flavourings" [] (flavours-rsp service))
    (GET  "/beers/flavourings/pick" req (flavours-pick-rsp service req))
    (POST "/beers/brew" req (brew-rsp service req))))

(defrecord Server [port]
  component/Lifecycle
  (start [this]
    (println ";; Starting Server")
    (assoc this :jetty
           (run-jetty (-> (myroutes this) hdr/api) {:port port}))))

(defn server [] (map->Server {:port 3002}))


;; =============================================================================
;; App
;; =============================================================================

(def app-components [:server :service :store])

(defrecord BeerApp [config-options store service server]
  component/Lifecycle
  (start [this] (component/start-system this app-components))
  (stop [this] (component/stop-system this app-components)))

(defn app [config-options]
  (let [{:keys [host port]} config-options]
    (map->BeerApp
      {:config-options config-options
       :store (store host port)
       :service (component/using (service config-options) {:store  :store})
       :server (component/using (server) [:service])})))


;; =============================================================================
;; Application entry point
;; =============================================================================

(defn -main [& args]
  (println "application entry point")
  (let [system
        (component/start (app {:host "myhost.com" :port 123}))]))
