(ns ^:dev/always decidotron.routes-spec
  (:require
    [fulcro-spec.core :refer [specification provided behavior assertions]]
    [decidotron.ui.routing :as routing]
    [bidi.bidi :as bidi]))

(specification "Discussion routes are resolved correctly"
  (behavior "for issues"
    (assertions
      "without slash"
      (bidi/match-route routing/app-routes "/discuss") => {:handler :issues}))
      ; (bidi/match-route routing/app-routes "/discuss/") => {:handler :issues} ; TODO allow trailing slash
  (behavior "for positions"
    (assertions "without slash"
      (bidi/match-route routing/app-routes "/discuss/cat-or-dog") => {:handler :positions
                                                                      :route-params {:slug "cat-or-dog"}}))
  (behavior "for attitudes"
    (assertions "without slash"
      (bidi/match-route routing/app-routes "/discuss/cat-or-dog/attitude/10") => {:handler :attitude
                                                                                  :route-params {:slug "cat-or-dog"
                                                                                                 :position "10"}})))
