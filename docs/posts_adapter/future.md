*   `onBindViewHolder` creates new `OnClickListener` lambdas for every bind; potential object churn. Use an interface or static listener reference.
*   Accessibility strings are constructed inside `bind`; move string concatenation to resources with placeholders (`getString(R.string.post_by_user, authorUsername)`).
*   `markwon` instance is nullable; consistency issue if some adapters have it and others don't.
*   `setupCardTouchListener` overrides `OnTouchListener`; interferes with ripple effects if not handled carefully (though `return false` allows propagation).
