(ns bedes-sleuth.ui
  (:require [om.core :as om :include-macros true]
            [om.dom :as omdom :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! >!]]
            [cljs-http.client :as http]))

(defn get-file
  "Returns a js/File instance or nil given the upload-form's owner node
   and a the ref of the file input."
  [owner ref]
  (let [node (om/get-node owner ref)]
    (let [file-list (.-files node)]
      (when (> (.-length file-list) 0)
        (aget file-list 0)))))

(defn upload-form [data owner]
  (reify
    om/IInitState
    (init-state [_]
      {:files (chan)})

    om/IRenderState
    (render-state [this {:keys [files]}]
      (dom/form
        (dom/input {:type "file" :name "filename" :size "40" :ref "file"})
        (dom/input {:type "submit" :value "upload"
                    :on-click (fn [e]
                                (.preventDefault e)
                                (when-let [file (get-file owner "file")]
                                  (put! files file)))})))))

(defn bedes-report [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div {:class "bedes-report"}
        (str "Your data is " (:score data) "% BEDES compliant!")
        (dom/h3 "Valid Headers")
        (dom/ul {:class "valid-list"}
                (for [header (:valid data)]
                  (dom/li {:class "valid-header"} header)))
        (dom/h3 "Invalid Headers")
        (dom/ul {:class "invalid-list"}
                (for [header (:invalid data)]
                  (dom/li {:class "invalid-header"} header)))))))
