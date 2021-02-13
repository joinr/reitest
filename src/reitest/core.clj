(ns reitest.core
  (:require [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.ring.coercion :as rrc]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [org.httpkit.server :as server]
            [ring.middleware.defaults :as defaults])
  (:gen-class))

(def state (atom {:sum 0}))
(defn get-sum [] (@state :sum))
(defn add-sum! [x]
  (let [new-sum (+ (@state :sum) x)
        _ (swap! state assoc :sum new-sum)]
    new-sum))
(defn reset-sum! [n] (swap! state assoc :sum n))

(def routes
  [
   ["/sum"
   {:get {:summary "Get the sum"
          :parameters {} #_{:query {:x int?, :y int?}}
          :responses {200 {:body {:sum int?}}}
          :handler (fn [_]
                     {:status 200
                      :body {:sum (get-sum)}})}
    :post {:summary "Reset the sum"
           :parameters {:query {:sum int?}}
           :responses {200 {:body {:sum int?}}}
           :handler (fn [{{{:keys [sum]} :query} :parameters}]
                     (reset-sum!)
                      {:status 200
                       :body {:sum sum}})}}]
   ["/add"
    {:post {:summary "Add to the sum"
            :parameters {:query {:x int?}}
            :responses {200 {:body {:sum int?}}}
            :handler (fn [{{{:keys [x]} :query} :parameters}]
                       {:status 200
                        :body {:sum (add-sum! x)}})}}]
    ["/math" {:get {:parameters {:query {:x int?, :y int?}}
                    :responses {200 {:body {:total pos-int?}}}
                    :handler (fn [{{{:keys [x y]} :query} :parameters}]
                               {:status 200
                                :body {:total (+ x y)}})}}]])

(def app
  (ring/ring-handler
   (ring/router
    routes
    ;; router data affecting all routes
    {:data {:coercion reitit.coercion.spec/coercion
            :middleware [rrc/coerce-exceptions-middleware
                         rrc/coerce-request-middleware
                         rrc/coerce-response-middleware]}})))

;;needed to get query strings parsed into params we can match.
(def wrapped
  (defaults/wrap-defaults app {:params    {:urlencoded true
                                           :keywordize true}}))

(defn -main
  "This is our main entry point"
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
                                        ; Run the server with Ring.defaults middleware
    (server/run-server #'wrapped {:port port})
                                        ; Run the server without ring defaults
                                        ;(server/run-server #'app-routes {:port port})
    (println (str "Running webserver at http:/127.0.0.1:" port "/"))))
