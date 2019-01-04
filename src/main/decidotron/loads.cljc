(ns decidotron.loads
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.data-fetch :as df]))

(defsc Issue [_ _]
  {:query [:dbas.issue/slug
           :dbas.issue/title
           :dbas.issue/summary
           :dbas.issue/description
           :dbas.issue/date
           :dbas.issue/url
           :dbas.issue/language]
   :ident [:dbas.issue/by-slug :dbas.issue/slug]})

(defsc Bubble [_ _]
  {:query [:html :text :type :url]})

(defsc Position [_ {:keys [url]}]
  {:query [:htmls :texts :url]
   :ident (fn []
            [:dbas.position/by-id (js/parseInt (nth (clojure.string/split url #"/") 3))])})

(defsc Positions [_ {:keys [positions]}]
  {:query [{:bubbles (prim/get-query Bubble)}
           {:positions (prim/get-query Position)}]
   :ident (fn []
            [:dbas.positions/by-slug (nth (clojure.string/split (-> positions first :url) #"/") 1)])})

(defn load-issues [component connection where]
  (df/load component :dbas/issues Issue
    {:remote :dbas
     :params {:connection connection}
     :parallel true
     :target where}))

(defn load-positions [component connection where slug]
  (df/load component :dbas.issue/positions Positions
    {:remote :dbas
     :params {:connection connection
              :slug slug}
     :parallel true
     :target where}))

(defsc Attitude [_ {:keys [url]}]
  {:query [:htmls :texts :url]})

(defsc Attitudes [_ props]
  {:query [{:bubbles (prim/get-query Bubble)}
           {:attitudes [{:agree (prim/get-query Attitude)}
                        {:disagree (prim/get-query Attitude)}
                        {:dontknow (prim/get-query Attitude)}]}]})


(defn load-attitudes [component connection where slug position]
  (df/load component :dbas.issue.position/attitudes Attitudes
    {:remote :dbas
     :params {:connection connection
              :slug slug
              :position position}
     :parallel true
     :target where}))