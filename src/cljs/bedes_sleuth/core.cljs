(ns bedes-sleuth.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [bedes-sleuth.bedes-schema :refer [bedes-headers]]
            [bedes-sleuth.ui :refer [upload-form bedes-report]]
            [cljs.core.async :refer [put! chan <! >!]]
            [cljs-http.client :as http]))

(enable-console-print!)

(def app-state (atom {:text "Hello BEDES!"
                      :upload-form {:action "/schema-csv"}}))

;; html5 file api
(defn file->str
  "Takes a js/File object and returns a channel containing a string
   of contents of file."
  [file]
  (let [result (chan)
        reader (js/FileReader.)]
    (set! (.-onload reader) (fn [_] (put! result (.-result reader))))
    (.readAsText reader file)
    result))

;; reading csv headers
;;
(defn csv-headers [csv-str]
  (js/console.log "csv-str called with:" csv-str)
  (-> csv-str
      (.split "\n")
      first
      (.split ",")))

;; scoring bedes headers
(def bedes? (set bedes-headers))

(defn check-bedes
  "Returns [valid-bedes-headers, invalid-bedes-headers] given a list
   of headers."
  [headers]
  [(filter bedes? headers)
   (filter (comp not bedes?) headers)])

(defn bedes-score
  "Returns the percentage of headers that are bedes compliant. "
  [valid invalid]
  (let [num-bedes (count valid)
        num-not (count invalid)
        total (+ num-bedes num-not)]
    (* 100
       (/ num-bedes total))))

(defn report-data
  [headers]
  (let [[valid invalid] (check-bedes headers)]
    {:valid valid
     :invalid invalid
     :score (bedes-score valid invalid)}))

;; posting to server
;;
(defn post-file
  "Returns a channel that will contain the post response."
  [file url]
  (js/console.log "post-file called")
  (let [form-data (js/FormData.)]
    (.append form-data "file" file)
    (http/post url {:body form-data})))

(defn app [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:files (chan)})

    om/IWillMount
    (will-mount [this]
      (let [files-chan (om/get-state owner :files)]
        (go (loop []
              (when-let [file (<! files-chan)]
                (let [headers (csv-headers (<! (file->str file)))
                      report (report-data headers)]
                  (js/console.log "updating app state")
                  (om/update! app :bedes-report report)
                  (recur)))))))

    om/IRenderState
    (render-state [_ {:keys [files]}]
      (dom/div {:class "app"}
        (dom/h1 (:text app))
        (dom/h2 "Are you BEDES compliant?")
        (om/build upload-form (:upload-form app) {:init-state {:files files}})
        (when-let [data (:bedes-report app)]
          (om/build bedes-report data))))))

(defn main []
  (om/root app app-state
           {:target (js/document.getElementById "app")}))
