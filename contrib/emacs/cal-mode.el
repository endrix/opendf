;;  This major-mode is derived from  the wpdl-mode.el provided
;;  by Scott Andrew Borton <scott-web@two-wugs.net> under the
;;  GPL.  
;;  Modifications Copyright (c) 2006-2007, Xilinx Inc.
;;  All rights reserved.
;;  
;; This program is free software; you can redistribute it and/or
;; modify it under the terms of the GNU General Public License as
;; published by the Free Software Foundation; either version 2 of
;; the License, or (at your option) any later version.

;; This program is distributed in the hope that it will be
;; useful, but WITHOUT ANY WARRANTY; without even the implied
;; warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
;; PURPOSE.  See the GNU General Public License for more details.
	
;; cal-mode.el --- A major mode for editing cal files
;;
;; This is a major mode for the CAL language (see sourceforge.net/projects/caltrop)
;; 
;; Features:
;; Basic syntax highlighting.
;;   Keywords
;;   variable declarations
;;   integer literals
;;   string literals
;;   reserved keywords
;;
;; Indentation
;; 

;;
;; Users can implement mode-specific customizations by providing this hook
;;
(defvar cal-mode-hook nil)

;;
;; Create a cal mode keymap for customizing the behavior of certain key combinations
;; 
(defvar cal-mode-map
  (let ((cal-mode-map (make-sparse-keymap)))
    ;; Activate feature of: when hitting enter cause the current line to auto-indent itself
    (define-key cal-mode-map "\C-c\C-c" 'comment-region)
    (define-key cal-mode-map "\C-c\C-u" 'uncomment-region)
    (define-key cal-mode-map "\C-j" 'reindent-then-newline-and-indent)
    (define-key cal-mode-map "\C-m" 'reindent-then-newline-and-indent)
    cal-mode-map)
  "Keymap for CAL major mode")

;;
;; Define the file extensions for which you want to use the cal mode
;; 
(add-to-list 'auto-mode-alist '("\\.cal\\'" . cal-mode))

;;
;; Syntax highlighting.
;;
;; Match the block level key words.
;;(regexp-opt '("actor" "action" "endactor" "endaction" "endpriority" "endschedule" "guard" "priority" "schedule") t)
;;(regexp-opt '("begin" "choose" "do" "else" "end" "endchoose" "endforeach" "endfunction" "endif" "endinitialize" "endlambda" "endlet" "endproc" "endprocedure" "endwhile" "for" "foreach" "fsm" "function" "if" "initialize" "lambda" "let" "map" "proc" "procedure" "schedule" "then" "while") t)
(defconst cal-font-lock-keywords-1
  (list
   '("\\<\\(act\\(?:ion\\|or\\)\\|end\\(?:act\\(?:ion\\|or\\)\\|priority\\|schedule\\)\\|guard\\|priority\\|schedule\\)\\>"
     . font-lock-builtin-face)
   '("\\<\\(begin\\|choose\\|do\\|e\\(?:lse\\|nd\\(?:choose\\|f\\(?:oreach\\|unction\\)\\|i\\(?:f\\|nitialize\\)\\|l\\(?:ambda\\|et\\)\\|proc\\(?:edure\\)?\\|while\\)?\\)\\|f\\(?:or\\(?:each\\)?\\|sm\\|unction\\)\\|i\\(?:f\\|nitialize\\)\\|l\\(?:ambda\\|et\\)\\|map\\|proc\\(?:edure\\)?\\|schedule\\|then\\|while\\)\\>"
     . font-lock-keyword-face)
   )
  "Minimal (block level) highlighting expressions for CAL mode")

;;
;; Match all the keywords
;; 
;;(regexp-opt '("all" "and" "any" "at" "delay" "div" "dom" "import" "in" "mod" "multi" "mutable" "not" "old" "or" "regexp" "repeat" "rng" "time" "var" ) t)
(defconst cal-font-lock-keywords-2
  (append cal-font-lock-keywords-1
	  (list
	   '("\\<\\(\\w*\\)[[:blank:]]*\\(:[[:blank:]]*\\[\\)[[:blank:]]*\\w*[[:blank:]]*\\(\\]\\)" (1 font-lock-variable-name-face) (2 font-lock-builtin-face) (3 font-lock-builtin-face))
	   '("\\<\\(actor\\)[[:blank:]]*\\([^[:blank:]]*\\)" (1 font-lock-keyword-face) (2 font-lock-variable-name-face))
	   '("\\(!=\\)\\|\\(:=\\)\\|\\(==>\\)" . font-lock-builtin-face)
	   '("\\<\\(a\\(?:ll\\|n[dy]\\|t\\)\\|d\\(?:elay\\|iv\\|om\\)\\|i\\(?:mport\\|n\\)\\|m\\(?:od\\|u\\(?:lti\\|table\\)\\)\\|not\\|o\\(?:ld\\|r\\)\\|r\\(?:e\\(?:gexp\\|peat\\)\\|ng\\)\\|time\\|var\\)\\>"
	     . font-lock-keyword-face)))
  "Additional keywords to highlight in CAL mode")

;;
;; Match CAL constants.  Both literals and string literals
;;
;;(regexp-opt '("const" "false" "null" "true") t)
;;(regexp-opt '([0-9]*) t)
(defconst cal-font-lock-keywords-3
  (append cal-font-lock-keywords-2
	  (list
	   '("\\<\\(const\\|false\\|null\\|true\\)\\>" . font-lock-string-face)
	   '("\\<[0-9]*\\>" . font-lock-constant-face)))
  "Constant/literal keywords in CAL mode")

;;
;; Match variable declaration constructs.
;;
(defconst cal-font-lock-keywords-4
  (append cal-font-lock-keywords-3
	  (list
	   '("\\<\\(bool\\|int\\)\\>" . font-lock-type-face)
	   ;; The int xxxx syntax
	   '("\\(int\\|bool\\)[ \t]+\\(\\w+\\)" (2 font-lock-variable-name-face))
	   ;; The int (size=lmnop) xxxx syntax
	   '("\\(int\\|bool\\)[ \t]*([^)]*)[ \t]*\\(\\w+\\)" (2 font-lock-variable-name-face))
	   ))
  "Matching variable declarations")

;;
;; Match the list of reserved keywords
;;
;; (regexp-opt '("assign" "case" "default" "endinvariant" "endtask" "endtype" "ensure" "invariant" "now" "out" "protocol" "require" "task" "type") t) 
(defconst cal-font-lock-keywords-5
  (append cal-font-lock-keywords-4
	  (list
	   '("\\<\\(assign\\|case\\|default\\|en\\(?:d\\(?:invariant\\|t\\(?:ask\\|ype\\)\\)\\|sure\\)\\|invariant\\|now\\|out\\|protocol\\|require\\|t\\(?:ask\\|ype\\)\\)\\>" . font-lock-warning-face)
	   ))
  "Matching reserved keywords")

(defvar cal-font-lock-keywords cal-font-lock-keywords-5
  "Default highlighting expressions for CAL mode")

(defvar cal-font-lock-syntactic-keywords
  (list  
   '("\\('\\).\\('\\)" (1 "\"") (2 "\"")))
  "Syntax-based highlighting specification for 'cal-mode'.")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;; Do indenting rules here
;;

;; Match "action" and "foobar: action"
(defconst named-action "\\([^: \t]+[ \t]*:[ \t]*action\\)" )
(defconst action-match (concat "action\\|" named-action) )

;; Match "initialize" (a special action)
(defconst initialize-match "initialize")

(defconst keywords-paired-with-end
  ;; action-match plus:
  ;; initialize-match plus:
  ;; "actor" "if" "function" "while" "procedure" "schedule" "priority" "foreach" "let"
  (concat "\\<\\(actor\\|function\\|if\\|while\\|procedure\\|schedule\\|priority\\|foreach\\|let\\|" action-match "\\|" initialize-match "\\)\\>")
  )
(defconst keywords-paired-with-end-rooted (concat "[ \t]*" keywords-paired-with-end) )

(defconst actor-level-element
  ;; "action" "function" "procedure" "schedule" "priority" "initialize"
  (concat "\\<\\(function\\|procedure\\|schedule\\|priority\\|" action-match "\\|" initialize-match "\\)\\>")
  )
(defconst actor-level-element-rooted (concat "[ \t]*" actor-level-element) )

(defconst indent-relative-to-actor-level-element
  ;; "var" "guard" "do" "begin"
  "\\<\\(var\\|guard\\|do\\|begin\\)\\>")
(defconst indent-relative-to-actor-level-element-rooted (concat "[ \t]*" indent-relative-to-actor-level-element) )


(defconst keywords-requiring-next-line-indent
  ;;(regexp-opt '("action" "actor" "var" "do" "guard" "if" "then" "else" "function" "while" "begin" "schedule" "priority" "foreach" "let") t)
  ;;"\\<\\(act\\(?:ion\\|or\\)\\|do\\|if\\|else\\|function\\|guard\\|then\\|var\\|while\\)\\>"
  ;;"\\<\\(act\\(?:ion\\|or\\)\\|begin\\|do\\|else\\|f\\(?:oreach\\|unction\\)\\|guard\\|if\\|let\\|priority\\|schedule\\|then\\|var\\|while\\)\\>"
  ;;(concat "\\<\\(actor\\|do\\|else\\|function\\|guard\\|then\\|var\\|while\\|begin\\|schedule\\|priority\\|foreach\\|" action-match "\\|" initialize-match "\\)\\>" )
  (concat "\\<\\(action\\|actor\\|var\\|do\\|guard\\|if\\|then\\|else\\|function\\|while\\|begin\\|schedule\\|priority\\|foreach\\|let\\|" action-match "\\|" initialize-match "\\)\\>" )
  )
(defconst keywords-requiring-next-line-indent-rooted (concat "[ \t]*" keywords-requiring-next-line-indent) )
(defun cal-indent-line ()
  (interactive)
  (beginning-of-line)

  ;; Start off indentation of the buffer at 0
  (if (bobp) (indent-line-to 0)
    (let((not-indented t) cur-indent)

      ;; Find the thing it pairs to and match the indent
      (if (looking-at "[ \t]*end")
	  (progn
	    (save-excursion
	      (goto-char (find-point-of-end-match (point)))
	      (setq cur-indent (current-indentation))
	      )
	    (setq not-indented nil)
	    )
				
	;; find preceding action, function, etc and indent relative to it
	(if (looking-at indent-relative-to-actor-level-element-rooted)
	    (save-excursion
	      (while not-indented
		(forward-line -1)
		(if (looking-at actor-level-element-rooted)
		    (progn (setq cur-indent (+ (current-indentation) default-tab-width))
			   (setq not-indented nil))
		  (if (bobp) (setq not-indented nil))
		  )
		)
	      )
					
	  ;; Enforce that certain things are always relative to the 'actor'.  This is kind-of a 'reset' for the indentation					
	  (if (looking-at actor-level-element-rooted)
	      (save-excursion
		(while not-indented
		  (forward-line -1)
		  (if (looking-at "\\<actor")
		      (progn (setq cur-indent (+ (current-indentation) default-tab-width))
			     (setq not-indented nil))
		    (if (bobp) (setq not-indented nil))
		    )
		  )
		)
						
	    ;; Special case for the else to keep it indented relative to a prior if, not the 'then'
	    (if (looking-at "[ \t]*else")
		(progn
		  (setq cur-indent (find-controlling-block-indent "\\<then" 0))
		  (setq not-indented nil)
		  )
							
	      ;; Default.  Find the containing block and indent relative to it.
	      (progn (setq cur-indent (find-controlling-block-indent keywords-requiring-next-line-indent default-tab-width))
		     (setq not-indented nil))
	      )
	    )
	  )
	)

      ;; Do the actual indentation
      (if cur-indent
	  (indent-line-to cur-indent)
	(indent-line-to 0)
	)
      )
    )
  )

(defun find-controlling-block-indent (match-terms offset)
  ;; Returns the indentation level of the line containing a match to the
  ;; 'match-terms' but skips over any intermediary blocks.  Adds 'offset'
  ;; to the indentation level of that line
  (save-excursion
    (let ((not-yet t) indent)
      (while not-yet
					;(forward-word -1)
	(if (not (cal-backward-word))
	    (if (looking-at match-terms)
		;; If we encounter something that demands an indent, take its indentation and add the offset
		(progn (setq indent (+ (current-indentation) offset))
		       (setq not-yet nil))
	      ;; If we get to the top of the buffer, punt and return 0
	      (if (bobp)
		  (progn (setq indent 0)
			 (setq not-yet nil))
		)
	      )
	  )
	)
      ;; The last 'form' evaluated is what is returned
      indent
      )
    )
  )

(defun find-point-of-end-match (start-position)
  ;; Returns the point of the keyword matching an assumed 'end' at the start position.
  "I'm not sure how efficient this is, since we search backwards word by word"
  (interactive)
  (goto-char start-position)
	
  (save-excursion
    (forward-word 1)
    (cal-backward-word)
    ;; return the current point
    (point)
    )
  )

(defun cal-backward-word ()
  ;; Moves backwards one word in a cal specific way, skipping over
  ;; code blocks (eg outermost if -> end pair) and comments
  ;; If 'skip-over-blocks' is non-nil then the returned point will be
  ;; one word prior to a close block.  If nil, the returned point will
  ;; be the start of any completed block.
  (let ((not-there-yet 0) (endcount 0) savedpoint (traversed-block nil))
    (while not-there-yet
					;(forward-comment -1)
      (forward-comment -100000)
      (forward-word -1)
      (if (looking-at "\\<action\\>")
	  ;; Special case.  Determine if this is a named action, and
	  ;; go back to the beginning of the name if it is.
	  (progn
	    (setq savedpoint (point))
	    ;; a custom forward-word that will skip over punctuation as well
	    ;; This allows us to have qualified action names
	    (skip-syntax-backward " ")
	    (skip-syntax-backward "w.")
	    (if (not (looking-at (concat named-action "\\>")))
		(goto-char savedpoint)
	      )
	    )
	)
      (if (looking-at "\\<end") (setq endcount (+ endcount 1))
	(if (looking-at keywords-paired-with-end) (setq endcount (- endcount 1)))
	)
      (if (<= endcount 0) (setq not-there-yet nil))
      (if (> endcount 0) (setq traversed-block t))
      )
    traversed-block
    )
  )

;;
;; Now update the syntax table
;;
(defvar cal-mode-syntax-table
  (let ((cal-mode-syntax-table (make-syntax-table)))
    ;; Allow an underscore in variable names
    (modify-syntax-entry ?_ "w" cal-mode-syntax-table)
    ;; Comment lines.  C/C++ style
    (modify-syntax-entry ?/ ". 124b" cal-mode-syntax-table)
    (modify-syntax-entry ?* ". 23" cal-mode-syntax-table)
    (modify-syntax-entry ?\n "> b" cal-mode-syntax-table)
    ;; define parentheses to match
    (modify-syntax-entry ?\( "()"  cal-mode-syntax-table)
    (modify-syntax-entry ?\) ")("  cal-mode-syntax-table)
    (modify-syntax-entry ?\[ "(]"  cal-mode-syntax-table)
    (modify-syntax-entry ?\] ")["  cal-mode-syntax-table)
    (modify-syntax-entry ?\{ "(}"  cal-mode-syntax-table)
    (modify-syntax-entry ?\} "){"  cal-mode-syntax-table)
    cal-mode-syntax-table)
  "Syntax table for cal-mode")

(define-derived-mode cal-mode fundamental-mode "Cal"
  "A major mode for editing Cal files."
  (set-syntax-table cal-mode-syntax-table)
  (set (make-local-variable 'comment-start) "// ")
  (set (make-local-variable 'comment-start-skip) "//+\\s-*")
  (set (make-local-variable 'font-lock-defaults)
       '(cal-font-lock-keywords nil t))
  (set (make-local-variable 'indent-line-function) 'cal-indent-line))

(provide 'cal-mode)




