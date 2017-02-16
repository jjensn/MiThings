/* JS */

/* Parallax Slider */
$(function() {
	$('#da-slider').cslider({
	  autoplay  : true,
	  interval : 8000
	});
});


/* Flex image slider */

$('.flex-image').flexslider({
  direction: "vertical",
  controlNav: false,
  directionNav: true,
  pauseOnHover: true,
  slideshowSpeed: 10000      
});

/* Testimonial slider */

$('.testi-flex').flexslider({
  direction: "vertical",
  controlNav: true,
  directionNav: false,
  pauseOnHover: true,
  slideshowSpeed: 8000      
});

/* About slider */

$('.about-flex').flexslider({
  direction: "vertical",
  controlNav: true,
  directionNav: false,
  pauseOnHover: true,
  slideshowSpeed: 8000      
});

/* Owl carousel */

/* Carousel */

$(document).ready(function() {
			
	 var recent = $("#owl-recent");
	 
	recent.owlCarousel({
		autoPlay: 3000, //Set AutoPlay to 3 seconds
		items : 4,
		mouseDrag : false,
		pagination : false
	});
	
	$(".next").click(function(){
			recent.trigger('owl.next');
	  })
	  
	  $(".prev").click(function(){
			recent.trigger('owl.prev');
	  })
});

/* Support */

$("#slist a").click(function(e){
   e.preventDefault();
   $(this).next('p').toggle(200);
});

/* Tooltip */

$('#price-tip1').tooltip();

/* Scroll to Top */

$(document).ready(function(){
  $(".totop").hide();

  $(function(){
    $(window).scroll(function(){
      if ($(this).scrollTop()>400)
      {
        $('.totop').fadeIn();
      } 
      else
      {
        $('.totop').fadeOut();
      }
    });

    $('.totop a').click(function (e) {
      e.preventDefault();
      $('body,html').animate({scrollTop: 0}, 500);
    });

  });
});


/* Portfolio filter */

/* Isotype */

// cache container
var $container = $('#portfolio');
// initialize isotope
$container.isotope();

// filter items when filter link is clicked
$('#filters a').click(function(){
  var selector = $(this).attr('data-filter');
  $container.isotope({ filter: selector });
  return false;
});

/* Pretty Photo for Gallery*/

jQuery(".prettyphoto").prettyPhoto({
	overlay_gallery: false, social_tools: false
});