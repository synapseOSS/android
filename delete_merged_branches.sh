#!/bin/bash

# Successfully merged branches to delete
merged_branches=(
    "chat-realtime-subscription-15000878287643291846"
    "chore-secure-onesignal-credentials-18066842922492748230"
    "feature-message-forwarding-ui-16956452752557662567"
    "feature-oauth-support-13637135772101466837"
    "migrate-chat-to-compose-m3-15340222604768733854"
    "notification-system-update-1491176133416946606"
    "realtime-read-receipts-10304203547174324374"
    "reels-adapter-init-14435374100296209888"
    "refactor-posts-adapter-fetching-8170451185715568676"
)

echo "Deleting ${#merged_branches[@]} merged branches..."

for branch in "${merged_branches[@]}"; do
    echo "Deleting remote branch: $branch"
    git push origin --delete "$branch"
done

echo "✅ All merged branches deleted from remote"
