package com.microsoft.xbox.telemetry.helpers;

import com.microsoft.xbox.telemetry.utc.model.UTCNames;
import com.microsoft.xbox.toolkit.XLEAssert;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * 07.01.2021
 *
 * @author <a href="https://github.com/timscriptov">timscriptov</a>
 */

public class UTCPeopleHub {
    public static CharSequence currentActivityTitle = "";
    public static String currentXUID = "";

    private static void verifyTrackedDefaults() {
        XLEAssert.assertFalse("Called trackPeopleHubView without set currentXUID", currentXUID.isEmpty());
        XLEAssert.assertFalse("Called trackPeopleHubView without set activityTitle", currentActivityTitle.toString().isEmpty());
    }

    public static @NotNull HashMap<String, Object> getAdditionalInfo(String str) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(UTCDeepLink.TARGET_XUID_KEY, "x:" + str);
        return hashMap;
    }

    public static void trackPeopleHubView(final CharSequence charSequence, final String str, final boolean z) {
        UTCEventTracker.callTrackWrapper(() -> {
            UTCPageView.track(z ? UTCNames.PageView.PeopleHub.PeopleHubMeView : UTCNames.PageView.PeopleHub.PeopleHubYouView, UTCPeopleHub.currentActivityTitle, UTCPeopleHub.getAdditionalInfo(str));
        });
    }

    public static void trackMute(boolean z) {
        verifyTrackedDefaults();
        trackMute(currentActivityTitle, currentXUID, z);
    }

    public static void trackMute(final CharSequence charSequence, final String str, final boolean z) {
        UTCEventTracker.callTrackWrapper(() -> {
            HashMap<String, Object> hashMap = UTCPeopleHub.getAdditionalInfo(str);
            hashMap.put("isMuted", z);
            UTCPageAction.track(UTCNames.PageAction.PeopleHub.Mute, charSequence, hashMap);
        });
    }

    public static void trackUnblock() {
        verifyTrackedDefaults();
        trackUnblock(currentActivityTitle, currentXUID);
    }

    public static void trackUnblock(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> UTCPageAction.track(UTCNames.PageAction.PeopleHub.Unblock, charSequence, UTCPeopleHub.getAdditionalInfo(str)));
    }

    public static void trackBlock() {
        verifyTrackedDefaults();
        trackBlock(currentActivityTitle, currentXUID);
    }

    public static void trackBlock(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> UTCPageAction.track(UTCNames.PageAction.PeopleHub.Block, charSequence, UTCPeopleHub.getAdditionalInfo(str)));
    }

    public static void trackBlockDialogComplete() {
        verifyTrackedDefaults();
        trackBlockDialogComplete(currentActivityTitle, currentXUID);
    }

    public static void trackBlockDialogComplete(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> UTCPageAction.track(UTCNames.PageAction.PeopleHub.BlockOK, charSequence, UTCPeopleHub.getAdditionalInfo(str)));
    }

    public static void trackReport() {
        verifyTrackedDefaults();
        trackReport(currentActivityTitle, currentXUID);
    }

    public static void trackReport(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> UTCPageAction.track(UTCNames.PageAction.PeopleHub.Report, charSequence, UTCPeopleHub.getAdditionalInfo(str)));
    }

    public static void trackViewInXboxApp() {
        verifyTrackedDefaults();
        trackViewInXboxApp(currentActivityTitle, currentXUID);
    }

    public static void trackViewInXboxApp(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> UTCPageAction.track(UTCNames.PageAction.PeopleHub.ViewXboxApp, charSequence, UTCPeopleHub.getAdditionalInfo(str)));
    }

    public static void trackViewInXboxAppDialogComplete() {
        verifyTrackedDefaults();
        trackViewInXboxAppDialogComplete(currentActivityTitle, currentXUID);
    }

    public static void trackViewInXboxAppDialogComplete(final CharSequence charSequence, final String str) {
        UTCEventTracker.callTrackWrapper(() -> UTCPageAction.track(UTCNames.PageAction.PeopleHub.ViewXboxAppOK, charSequence, UTCPeopleHub.getAdditionalInfo(str)));
    }
}
