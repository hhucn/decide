(ns decidotron.loads
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.data-fetch :as df]
    [fulcro.client.mutations :as m :refer [defmutation]]))

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
    {:remote   :dbas
     :params   {:connection connection}
     :parallel true
     :target   where}))

(defn- set-bubbles* [state bubbles]
  (assoc-in state [:PAGE.discuss/dialog 1 :bubbles] bubbles)) ; Push to dynamic target in the future!

(defmutation set-bubbles [{:keys [bubbles]}]
  (action [{:keys [state]}] (swap! state set-bubbles* bubbles)))

(defn- move-bubbles-to-dialog* [state edge]
  (js/console.log state)
  (js/console.log edge)
  (set-bubbles* state (get-in state edge)))

(defmutation move-bubbles-to-dialog [{:keys [edge]}]
  (action [{:keys [state]}]
    (swap! state move-bubbles-to-dialog* edge)))

(defmutation ensure-positions [{:keys [set-bubbles?] :or {set-bubbles? false}}]
  (action [{:keys [ref state] :as env}]
    (let [state*     @state
          connection (:dbas/connection state*)
          slug       (get-in state* [:root/current-page :positions :route-params :slug])
          target (conj ref :positions)]
      (when-not (get-in state* (conj ref :positions) false)
        (df/load-action env :dbas.issue/positions Positions
          (merge
            {:remote               :dbas
             :params               {:connection connection
                                    :slug       slug}
             :target target}
            (when set-bubbles?
             {:post-mutation        `move-bubbles-to-dialog
              :post-mutation-params {:edge [:dbas.positions/by-slug slug :bubbles]}
              :refresh              [[:PAGE.discuss/dialog 1]]}))))))
  (dbas [env] (df/remote-load env)))

(defsc Attitude [_ {:keys [url]}]
  {:query [:htmls :texts :url]})

(defsc Attitudes [_ props]
  {:query [{:bubbles (prim/get-query Bubble)}
           {:attitudes [{:agree (prim/get-query Attitude)}
                        {:disagree (prim/get-query Attitude)}
                        {:dontknow (prim/get-query Attitude)}]}]})


(defn load-attitudes [component connection where slug position]
  (df/load component :dbas.issue.position/attitudes Attitudes
    {:remote   :dbas
     :params   {:connection connection
                :slug       slug
                :position   position}
     :parallel true
     :target   where}))