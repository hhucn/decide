(ns ^:dev/always decidotron.routes-spec
  (:require
    [fulcro-spec.core :refer [specification provided behavior assertions]]
    [decidotron.ui.routing :as routing]
    [bidi.bidi :as bidi]))

(specification "Discussion routes"
  (behavior "are resolved correctly"
    (assertions
      "for discussion root"
      (bidi/match-route routing/app-routes "/discuss") => {:handler :issues}
      ; (bidi/match-route routing/app-routes "/discuss/") => {:handler :issues} ; TODO allow trailing slash
      "for discussion with slug"
      (bidi/match-route routing/app-routes "/discuss/cat-or-dog") => {:handler :positions
                                                                      :route-params {:slug "cat-or-dog"}})))
