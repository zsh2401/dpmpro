/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.commands.dpm;

import android.accounts.Account;
import android.accounts.IAccountManager;
import android.accounts.IAccountManagerResponse;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.IUserManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import com.android.internal.os.BaseCommand;

import java.io.PrintStream;
import java.util.List;

public final class Dpm extends BaseCommand {

    /**
     * Command-line entry point.
     *
     * @param args The command-line arguments
     */
    public static void main(String[] args) {
        (new Dpm()).run(args);
    }

    private static final String COMMAND_SET_ACTIVE_ADMIN = "set-active-admin";
    private static final String COMMAND_SET_DEVICE_OWNER = "set-device-owner";
    private static final String COMMAND_SET_PROFILE_OWNER = "set-profile-owner";
    private static final String COMMAND_REMOVE_ACTIVE_ADMIN = "remove-active-admin";

    private static final String COMMAND_REMOVE_ALL_USERS = "remove-all-users";
    private static final String COMMAND_REMOVE_ALL_ACCOUNTS = "remove-all-accounts";

    private IDevicePolicyManager mDevicePolicyManager;
    private IUserManager mUserManager;
    private IAccountManager mAccountManager;

    private int mUserId = UserHandle.USER_SYSTEM;
    private String mName = "";
    private ComponentName mComponent = null;

    @Override
    public void onShowUsage(PrintStream out) {
        out.println("Android Command: Dpm Pro 1.0\n" +
                "Compiled by web1n.\n" +
                "\n" +
                "usage: dpm [subcommand] [options]\n" +
                "usage: dpm set-active-admin [ --user <USER_ID> | current ] <COMPONENT>\n" +
                // STOPSHIP Finalize it
                "usage: dpm set-device-owner [ --user <USER_ID> | current *EXPERIMENTAL* ] " +
                "[ --name <NAME> ] <COMPONENT>\n" +
                "usage: dpm set-profile-owner [ --user <USER_ID> | current ] [ --name <NAME> ] " +
                "<COMPONENT>\n" +
                "usage: dpm remove-active-admin [ --user <USER_ID> | current ] [ --name <NAME> ] " +
                "<COMPONENT>\n" +
                "usage: dpm remove-all-users\n" +
                "usage: dpm remove-all-accounts\n" +
                "\n" +
                "dpm set-active-admin: Sets the given component as active admin" +
                " for an existing user.\n" +
                "\n" +
                "dpm set-device-owner: Sets the given component as active admin, and its" +
                " package as device owner.\n" +
                "\n" +
                "dpm set-profile-owner: Sets the given component as active admin and profile" +
                " owner for an existing user.\n" +
                "\n" +
                "dpm remove-active-admin: Disables an active admin, the admin must have declared" +
                " android:testOnly in the application in its manifest. This will also remove" +
                " device and profile owners.\n" +
                "\n" +
                "dpm remove-all-users: Removes all existing users.\n" +
                "\n" +
                "dpm remove-all-accounts: Removes all existing accounts. Device should not" +
                " have screen lock.\n");
    }

    @Override
    public void onRun() throws Exception {
        System.out.print("Android Command: Dpm Pro 1.0\n" +
                "Compiled by web1n.\n" +
                "\n");

        mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(
                ServiceManager.getService(Context.DEVICE_POLICY_SERVICE));
        mUserManager = IUserManager.Stub.asInterface(
                ServiceManager.getService(Context.USER_SERVICE));
        mAccountManager = IAccountManager.Stub.asInterface(
                ServiceManager.getService(Context.ACCOUNT_SERVICE));

        if (mDevicePolicyManager == null) {
            showError("Error: Could not access the Device Policy Manager. Is the system running?");
            return;
        }
        if (mUserManager == null) {
            showError("Error: Could not access the User Manager. Is the system running?");
            return;
        }
        if (mAccountManager == null) {
            showError("Error: Could not access the Account Manager. Is the system running?");
            return;
        }

        String command = nextArgRequired();
        switch (command) {
            case COMMAND_SET_ACTIVE_ADMIN:
                runSetActiveAdmin();
                break;
            case COMMAND_SET_DEVICE_OWNER:
                runSetDeviceOwner();
                break;
            case COMMAND_SET_PROFILE_OWNER:
                runSetProfileOwner();
                break;
            case COMMAND_REMOVE_ACTIVE_ADMIN:
                runRemoveActiveAdmin();
                break;
            case COMMAND_REMOVE_ALL_ACCOUNTS:
                runRemoveAllAccounts();
                break;
            case COMMAND_REMOVE_ALL_USERS:
                runRemoveAllUsers();
                break;
            default:
                throw new IllegalArgumentException("unknown command '" + command + "'");
        }
    }

    private void parseArgs(boolean canHaveName) {
        String opt;
        while ((opt = nextOption()) != null) {
            if ("--user".equals(opt)) {
                String arg = nextArgRequired();
                if ("current".equals(arg) || "cur".equals(arg)) {
                    mUserId = UserHandle.USER_CURRENT;
                } else {
                    mUserId = parseInt(arg);
                }
                if (mUserId == UserHandle.USER_CURRENT) {
                    IActivityManager activityManager = ActivityManager.getService();
                    try {
                        mUserId = activityManager.getCurrentUser().id;
                    } catch (RemoteException e) {
                        e.rethrowAsRuntimeException();
                    }
                }
            } else if (canHaveName && "--name".equals(opt)) {
                mName = nextArgRequired();
            } else {
                throw new IllegalArgumentException("Unknown option: " + opt);
            }
        }
        mComponent = parseComponentName(nextArgRequired());
    }

    private void runSetActiveAdmin() throws RemoteException {
        parseArgs(/*canHaveName=*/ false);
        mDevicePolicyManager.setActiveAdmin(mComponent, true /*refreshing*/, mUserId);

        System.out.println("Success: Active admin set to component " + mComponent.toShortString());
    }

    private void runSetDeviceOwner() throws RemoteException {
        parseArgs(/*canHaveName=*/ true);
        mDevicePolicyManager.setActiveAdmin(mComponent, true /*refreshing*/, mUserId);

        try {
            if (!mDevicePolicyManager.setDeviceOwner(mComponent, mName, mUserId)) {
                throw new RuntimeException(
                        "Can't set package " + mComponent + " as device owner.");
            }
        } catch (Exception e) {
            // Need to remove the admin that we just added.
            mDevicePolicyManager.removeActiveAdmin(mComponent, UserHandle.USER_SYSTEM);
            throw e;
        }

        mDevicePolicyManager.setUserProvisioningState(
                DevicePolicyManager.STATE_USER_SETUP_FINALIZED, mUserId);

        System.out.println("Success: Device owner set to package " + mComponent);
        System.out.println("Active admin set to component " + mComponent.toShortString());
    }

    private void runRemoveActiveAdmin() throws RemoteException {
        parseArgs(/*canHaveName=*/ false);
        mDevicePolicyManager.forceRemoveActiveAdmin(mComponent, mUserId);
        System.out.println("Success: Admin removed " + mComponent);
    }

    private void runRemoveAllUsers() throws RemoteException {
        // parseArgs(/*canHaveName=*/ false);

        List<UserInfo> users = mUserManager.getUsers(true);
        if (users == null) {
            showError("Error: Can not get all users. Is the system running?");
            return;
        }

        for (UserInfo userInfo : users) {
            if (userInfo.id == 0) continue; //User 0 is no need to remove.

            mUserManager.removeUser(userInfo.id);
            System.out.println("Success: User removed: " + userInfo.toString());
        }
    }

    private void runRemoveAllAccounts() throws RemoteException {
        // parseArgs(/*canHaveName=*/ false);

        Account[] accounts = mAccountManager.getAccountsAsUser(null, mUserId, "com.android.shell");
        if (accounts == null) {
            showError("Error: Can not get all accounts. Is the system running?");
            return;
        }

        for (final Account account : accounts) {
            mAccountManager.removeAccountAsUser(new IAccountManagerResponse.Stub() {
                @Override
                public void onResult(Bundle bundle) {

                }

                @Override
                public void onError(int i, String s) {

                }
            }, account, true, 0);

        }
    }

    private void runSetProfileOwner() throws RemoteException {
        parseArgs(/*canHaveName=*/ true);
        mDevicePolicyManager.setActiveAdmin(mComponent, true /*refreshing*/, mUserId);

        try {
            if (!mDevicePolicyManager.setProfileOwner(mComponent, mName, mUserId)) {
                throw new RuntimeException("Can't set component " + mComponent.toShortString() +
                        " as profile owner for user " + mUserId);
            }
        } catch (Exception e) {
            // Need to remove the admin that we just added.
            mDevicePolicyManager.removeActiveAdmin(mComponent, mUserId);
            throw e;
        }

        mDevicePolicyManager.setUserProvisioningState(
                DevicePolicyManager.STATE_USER_SETUP_FINALIZED, mUserId);

        System.out.println("Success: Active admin and profile owner set to "
                + mComponent.toShortString() + " for user " + mUserId);
    }

    private ComponentName parseComponentName(String component) {
        ComponentName cn = ComponentName.unflattenFromString(component);
        if (cn == null) {
            throw new IllegalArgumentException("Invalid component " + component);
        }
        return cn;
    }

    private int parseInt(String argument) {
        try {
            return Integer.parseInt(argument);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer argument '" + argument + "'", e);
        }
    }

}
