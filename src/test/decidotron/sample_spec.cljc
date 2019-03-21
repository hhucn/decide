(ns decidotron.sample-spec
  (:require
    [fulcro-spec.core :refer [specification provided behavior assertions]]
    #?(:cljs [decidotron.ui.routing :as r])))

#?(:cljs
   (specification "URL (dis-)assembly Spec"
     (behavior "disassembling of URLs works"
       (assertions
         "with the empty string"
         (r/url->route "") => [""]
         "just with a slash"
         (r/url->route "/") => [""]
         "with a single segment"
         (r/url->route "/foo") => ["foo"]
         "with multiple segments"
         (r/url->route "/foo/bar") => ["foo" "bar"]))

     (behavior "assembling of URLs works"
       (assertions
         "with an empty string"
         (r/route->url [""]) => "/"
         "with an empty path"
         (r/route->url []) => "/"
         "with a single segment"
         (r/route->url ["foo"]) => "/foo"
         "with multiple segments"
         (r/route->url ["foo" "bar"]) => "/foo/bar"))))
