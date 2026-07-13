package com.aislego.growth.dto;

import java.util.List;

public record ReferralSummaryResponse(
        String referralCode,
        long invitedFriends,
        long rewardedFriends,
        List<String> rewardCouponCodes
) {
}
