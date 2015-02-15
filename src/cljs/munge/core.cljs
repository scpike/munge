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

(def code (atom "def munge(x)
  x.upcase
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

(defn codify
  [s]
  (js/eval (js/Opal.compile s)))

(defn munge
  [& e]
  (let [f-js (codify @code)
        f #(js/Opal.Object.$munge %)
        results (map f (clojure.string/split @input #"\n" ))]
    (reset! result (->> results
                       (remove clojure.string/blank?)
                       (clojure.string/join  "\n")))))


(defn make-codemirror
  [text-area-node]
  ; Have to use js-obj, passing a map as opts doesn't work
  (let [opts (js-obj "mode" "ruby" "lineNumbers" true "keyMap" "emacs")
        cm (js/CodeMirror.fromTextArea text-area-node opts )]
    (.on cm "change" #(reset! code (-> % .getValue)))
    ))

(def codemirror
  (with-meta identity
    {:component-did-mount #(make-codemirror (reagent/dom-node %))}))

(defn code-input []
  (fn []
    [codemirror
     [:textarea {:rows 20
                 :cols 40
                 :defaultValue @code }]]))

(defn munge-it []
  [:div { :class "container" } [:h2 "Munge yourself some data"]
   [:h2 "Code"]
   [:p "Write some ruby which defines a function `munge`."]
   [:div { :class "row" }
    [:div { :class "col-md-12"} [code-input]]]
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
