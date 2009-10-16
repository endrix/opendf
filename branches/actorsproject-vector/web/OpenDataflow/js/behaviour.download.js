var jQueryUI = {};

function build() {
	
	$("div.click-to-download").slice(0,2).animate({ opacity: 0 }, 500);
	
	$("div.click-to-download").slice(2,3).animate({ left: "-=627px" }, 500);
	
	$("div.builder").css({ display: 'block', opacity: 0 }).animate({ opacity: 1, top: "-=200px" }, 500);
	
	$(".download").animate({ height: "+=330px" }, 500);
	
	$('a.download').attr('href', 'javascript:download();');
	
};

function download() {
	if ($('#components-fm tr :checkbox:checked').size() <= 0) {
		alert("You should select at least one component.");
		return;
	}
	$('#components-fm').submit();
};

var updateDownloadFileSize = function() {
	
	window.selected_version = $("#version option")[$("#version")[0].selectedIndex].value;
	filterVersion();
	
	
	var total = 0;
	$('.components-list .list-component :checkbox:checked').each(function() {
		total += parseFloat($(this).attr('class'), 10);
	});
	
	$('#total-size-normal').text(total.toFixed(2) + ' Kb');
	$('#total-size-packed').text((total*({ packed: 0.35, yui: 0.66, jsmin: 0.66, normal: 1 }[$('#compression').val() || 'packed'])).toFixed(2) + ' Kb');
};

var filterVersion = function() {
	
	$("div.components-list > div").each(function() {

		if(selected_version < $(this).attr("version")) {
			$(this).hide();
			$(this).next().hide();
		} else {
			$(this).show();
			$(this).next().show();
		}
			
		
	});
	
};
	

$(document).ready(function() {
	
	var dependency_checkall = function(cname) {
		if (jQueryUI && jQueryUI.dependencies) {
			$.each(jQueryUI.dependencies[cname], function(i, v) {
				$(".components-list :checkbox[value='"+v+"']")
					.attr('checked', 'checked').parents('.list-component').addClass('list-component-selected')
						.find('.jquery-safari-checkbox-box').addClass('jquery-safari-checkbox-checked');
			});
		}
	};
	
	$(".components-list :checkbox").removeAttr('checked');
	
	$('.components-list .list-component:has(input)')
		.bind('mouseenter', function() {
			$(this).effect('highlight', { color: '#fff', duration: 'fast' });
		})
		.mouseup(function(e) {
			var t = e.target;
			var cb = $(":checkbox:first", this), cname = cb.val();
			
			$(this).removeClass('list-component-selected')
				.find('.jquery-safari-checkbox-box').removeClass('jquery-safari-checkbox-checked');
			
			if (!cb.is(':checked')) {
				dependency_checkall(cname);
				$(this).addClass('list-component-selected')
					.find('.jquery-safari-checkbox-box').addClass('jquery-safari-checkbox-checked');
			}
			
			if (t && /img/i.test(t.nodeName)) return;
			$(this).toggleCheckboxes(":checkbox:first");
			//$(this).effect('highlight');
		});
	
	$('#compression').selectbox();
	$('#version').selectbox();
	
	$('#compression_container li').click(function() {
		//console.log($(this).attr('id').replace('compression_input_', ''))
	});
	
	
	
	
	//Prepare the version box
	$("#version option")[0].selected = true;
	window.selected_version = $("#version option")[0].value;
	
	
	updateDownloadFileSize();
	
	
	$('.components-list').click(function() { updateDownloadFileSize(); });
	
	$('.components-list :checkbox, .components-list :radio').checkbox({
		empty: '/images/empty.png', cls: 'jquery-safari-checkbox'
	});

	$('#selectall-components').toggle(
		function() {
			$('.list-component').addClass('list-component-selected');
			$('.list-component :checkbox').attr('checked', 'checked');
			$(this).text('Unselect all components');
			updateDownloadFileSize();
		},
		function() {
			$('.list-component').removeClass('list-component-selected');
			$('.list-component :checkbox').removeAttr('checked');
			$(this).text('Select all components');
			updateDownloadFileSize();
		}
	);
	
});