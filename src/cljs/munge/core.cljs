(ns munge.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react])
    (:import goog.History))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to munge"]
   [:div
    [:a {:href "#/about"} "go to about page"]
    [:span " · "]
    [:a {:href "#/munge-it"} "munge something"]]])

(defn about-page []
  [:div [:h2 "About munge"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(def map-code (atom "def mapfn(x)
  x.downcase.gsub(/[^\\w\\s]/, '').gsub(' ', '_')
end"))

(def filter-code (atom "def filterfn(x)
  x =~ /.*/
end"))

(def input (atom "New York, New York
Los Angeles, California
Chicago, Illinois
Houston, Texas
Philadelphia, Pennsylvania
Phoenix, Arizona
San Antonio, Texas
San Diego, California
Dallas, Texas
San Jose, California
Austin, Texas
Indianapolis, Indiana
Jacksonville, Florida
San Francisco, California
Columbus, Ohio
Charlotte, North Carolina
Fort Worth, Texas
Detroit, Michigan
El Paso, Texas
Memphis, Tennessee
Seattle, Washington
Denver, Colorado
Washington, District of Columbia
Boston, Massachusetts
Nashville, Tennessee
Baltimore, Maryland
Oklahoma City, Oklahoma
Louisville, Kentucky
Portland, Oregon"))

(def result (atom ""))

(defn codify-map
  [s]
  (js/eval (js/Opal.compile s)))

(defn codify-filter
  [s]
  (js/eval (js/Opal.compile s)))

(defn filter-with-nil
  [f coll]
  (println (f (first coll)))
  (filter #(and (f %)
                (not= js/Opal.NilClass (.-_klass (f %))))
          coll))

(defn munge
  [& e]
  (let [f-js (codify-map @map-code)
        filter-js (codify-filter @filter-code)
        mapfn #(js/Opal.Object.$mapfn %)
        filterfn #(js/Opal.Object.$filterfn %)
        input (clojure.string/split @input #"\n" )]
    (reset! result (->> input
                        (filter-with-nil filterfn)
                        (map mapfn)
                        (remove clojure.string/blank?)
                        (clojure.string/join  "\n")))))
(munge)

(defn make-codemirror
  [atm text-area-node]
  ; Have to use js-obj, passing a map as opts doesn't work
  (let [opts (js-obj "mode" "ruby" "lineNumbers" true "keyMap" "emacs")
        cm (js/CodeMirror.fromTextArea text-area-node opts )]
    (.on cm "change" #(reset! atm (-> % .getValue)))))

(defn codemirror
  [atm]
  (with-meta identity
    {:component-did-mount #(make-codemirror atm (reagent/dom-node %))}))

(defn code-input [atm]
  [(codemirror atm)
   [:textarea {:rows 20
               :cols 40
               :defaultValue (deref atm) }]])

(def map-code-input
  (code-input map-code))

(def filter-code-input
  (code-input filter-code))

(defn munge-it []
  [:div { :class "container" } [:h1 "Munge yourself some data"]
   [:div { :class "row" }
    [:div {:class "col-md-6"}
     [:h2 "Filter"]
     [:p "Return true to allow a row through."]
     filter-code-input]
    [:div {:class "col-md-6"}
     [:h2 "Map"]
     [:p "Write some ruby which defines a function `mapfn`."]
     map-code-input]]
   [:div { :class "row" }
    [:div { :class "col-md-12"} [:input {:type "button" :value "Munge away!"
                                         :class "btn-default btn"
                                         :on-click munge}]]]
   [:h2 "Results"]
   [:div { :class "row form-group" }
    [:div { :class "col-md-6" }
     [:label "Input"]
     [:textarea {:rows 40  :value @input :class "form-control"
                  :on-change #(reset! input (-> % .-target .-value))}]]
    [:div  { :class "col-md-6" }
     [:label "Output"]
     [:textarea {:rows 40  :disabled true :value @result :class "form-control"
                  :on-change #(reset! result (-> % .-target .-value))}]]]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/munge-it" []
  (session/put! :current-page #'munge-it))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn init! []
  (hook-browser-navigation!)
  (reagent/render-component [current-page] (.getElementById js/document "app")))
