(ns reitest.core
  (:require [reitit.ring :as ring]
            [reitit.coercion.spec]
            [reitit.ring.coercion :as rrc]
            [org.httpkit.server :as server]
            [clojure.data.json :as js]
            [ring.middleware.defaults :as defaults])
  (:gen-class))

(defn coerce [js-data & {:keys [keywordize?]}]
  (try (js/read-str js-data :key-fn (if keywordize? keyword identity))
       (catch Exception e "malformed JSON data!")))

(def routes
  [
   ["/coerce"
   {:get {:summary "json->edn"
          :parameters {:query {:json string? :keywordize boolean?}}
          :responses {200 {:body {:edn any?}}}
          :handler (fn [{{{:keys [json keywordize]} :query} :parameters}]
                     {:status 200
                      :body {:edn (coerce json :keywordize? keywordize)}})}}]])

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
