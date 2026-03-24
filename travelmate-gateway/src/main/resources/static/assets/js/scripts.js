document.addEventListener('click', function(event) {
  // Close details.dropdown when clicking outside
  const openDropdowns = document.querySelectorAll('details.dropdown[open]');
  openDropdowns.forEach(function(details) {
    if (!details.contains(event.target)) {
      details.removeAttribute('open');
      toggleDropdownBackdrop(false);
    }
  });
});

// Manage dropdown backdrop and mobile state
document.addEventListener('DOMContentLoaded', function() {
  document.querySelectorAll('details.dropdown > summary').forEach(summary => {
    summary.addEventListener('click', function(e) {
      const details = this.parentElement;
      const isOpen = details.hasAttribute('open');
      
      // If we are opening this dropdown, close others
      if (!isOpen) {
        document.querySelectorAll('details.dropdown[open]').forEach(d => {
          if (d !== details) d.removeAttribute('open');
        });
        
        // Show backdrop on mobile
        if (window.innerWidth <= 768) {
          toggleDropdownBackdrop(true);
        }
      } else {
        toggleDropdownBackdrop(false);
      }
    });
  });

  const backdrop = document.getElementById('dropdown-backdrop');
  if (backdrop) {
    backdrop.addEventListener('click', function() {
      document.querySelectorAll('details.dropdown[open]').forEach(d => d.removeAttribute('open'));
      toggleDropdownBackdrop(false);
    });
  }
});

function toggleDropdownBackdrop(show) {
  const backdrop = document.getElementById('dropdown-backdrop');
  if (backdrop) {
    backdrop.style.display = show ? 'block' : 'none';
  }
  if (window.innerWidth <= 768) {
    document.body.classList.toggle('dropdown-open', show);
  }
}

function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.showModal();
    document.documentElement.classList.add('modal-is-open');
  }
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) {
    modal.close();
    document.documentElement.classList.remove('modal-is-open');
  }
}
