$(document).ready(function() {
	
	// smooth hover effects by DragonInteractive
	var hover = hoverEffects();
	hover.init();

});
	

/**
 * All credit here goes to DragonInteractive and Yuri Vishnevsky
 */
var hoverEffects = function() {
	var me = this;
	var args = arguments;
	var self = {
		c: {
			navItems: ' #launch-pad .launch-pad-button',
			navSpeed: ($.browser.safari ? 600: 350),
			snOpeningSpeed: ($.browser.safari ? 400: 250),
			snOpeningTimeout: 150,
			snClosingSpeed: function() {
				if (self.subnavHovered()) return 123450;
				return 150
			},
			snClosingTimeout: 700
		},
		init: function() {
			//$('.bg', this.c.navItems).css({
			//	'opacity': 0
			//});
			this.initHoverFades()
		},
		subnavHovered: function() {
			var hovered = false;
			$(self.c.navItems).each(function() {
				if (this.hovered) hovered = true
			});
			return hovered
		},
		initHoverFades: function() {
			//$('#navigation .bg').css('opacity', 0);
			$(self.c.navItems).hover(function() {
				self.fadeNavIn.apply(this)
			},
			function() {
				var el = this;
				setTimeout(function() {
					if (!el.open) self.fadeNavOut.apply(el)
				},
				10)
			})
		},
		fadeNavIn: function() {
			$('.bg', this).stop().animate({
				'opacity': 1
			},
			self.c.navSpeed)
		},
		fadeNavOut: function() {
			$('.bg', this).stop().animate({
				'opacity': 0
			},
			self.c.navSpeed)
		},
		initSubmenus: function() {
			$(this.c.navItems).hover(function() {
				$(self.c.navItems).not(this).each(function() {
					self.fadeNavOut.apply(this);
				});
				this.hovered = true;
				var el = this;
				self.fadeNavIn.apply(el);
			},
			function() {
				this.hovered = false;
				var el = this;
				if (!el.open) self.fadeNavOut.apply(el);
			})
		}
	};
	
	return self;
};
