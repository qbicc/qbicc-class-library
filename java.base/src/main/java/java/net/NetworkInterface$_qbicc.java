/*
 * This code is based on OpenJDK source file(s) which contain the following copyright notice:
 *
 * ------
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 * ------
 *
 * This file may contain additional modifications which are Copyright (c) Red Hat and other
 * contributors.
 */

package java.net;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.bsd.Ifaddrs.*;
import static org.qbicc.runtime.bsd.NetIf.*;
import static org.qbicc.runtime.linux.NetIf.*;
import static org.qbicc.runtime.posix.ArpaInet.*;
import static org.qbicc.runtime.posix.Errno.*;
import static org.qbicc.runtime.posix.SysIoctl.*;
import static org.qbicc.runtime.posix.NetinetIn.*;
import static org.qbicc.runtime.posix.NetIf.*;
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.posix.Unistd.*;
import static org.qbicc.runtime.stdc.Errno.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Stdlib.*;
import static org.qbicc.runtime.stdc.String.*;

import org.qbicc.rt.annotation.Tracking;
import org.qbicc.runtime.Build;

@Tracking("src/java.base/unix/native/libnet/NetworkInterface.c")
@Tracking("src/java.base/windows/native/libnet/NetworkInterface.c")
class NetworkInterface$_qbicc {

    static class netaddr {
        ptr<struct_sockaddr> addr;
        ptr<struct_sockaddr> brdcast;
        c_short mask;
        c_int family;
        netaddr next;
    }

    static class netif {
        char_ptr name;
        c_int index;
        boolean virtual;
        netaddr addr;
        netif childs;
        netif next;
    }

    /*
     * Frees the malloced struct_sockaddrs and char_ptrs for an interface list (including any attached addresses).
     */
    private static void freeif(netif ifs) {
        for (netif currif = ifs; currif != null; currif = currif.next) {
            for (netaddr addrP = currif.addr; addrP != null; addrP = addrP.next) {
                free(addrP.addr.cast());
                free(addrP.brdcast.cast());
                addrP.addr = word(0);
                addrP.brdcast = word(0);
            }
            if (currif.childs != null) {
                freeif(currif.childs);
            }
            free(ifs.name.cast());
            ifs.name = word(0);
        }
    }

    static netif addif(c_int sock, const_char_ptr if_name, netif ifs,
                       ptr<struct_sockaddr> ifr_addrP, ptr<struct_sockaddr> ifr_broadaddrP,
                       c_int family, c_short prefix) {
        netif currif = ifs;
        c_char[] name = new c_char[IF_NAMESIZE.intValue()];
        c_char[] vname = new c_char[IF_NAMESIZE.intValue()];
        boolean isVirtual = false;

        // If the interface name is a logical interface then we remove the unit
        // number so that we have the physical interface (eg: hme0:1 -> hme0).
        // NetworkInterface currently doesn't have any concept of physical vs.
        // logical interfaces.
        strncpy(addr_of(name[0]), if_name, IF_NAMESIZE.cast());
        name[IF_NAMESIZE.intValue() - 1] = word(0);
        vname[0] = word(0);

        // Create and populate the netaddr node.
        netaddr addrP = new netaddr();
        size_t addr_size = (family == AF_INET) ? sizeof(struct_sockaddr_in.class) : sizeof(struct_sockaddr_in6.class);
        addrP.addr = malloc(addr_size);
        if (addrP.addr.isNull()) {
            return ifs; // match OpenJDK behavior: if malloc fails just return the unmodified netif
        }
        memcpy(addrP.addr.cast(), ifr_addrP.cast(), addr_size);
        addrP.family = family;
        addrP.mask = prefix;
        addrP.next = null;

        // for IPv4 add broadcast address
        if (family == AF_INET && !ifr_broadaddrP.isNull()) {
            addrP.brdcast = malloc(addr_size);
            if (addrP.brdcast.isNull()) {
                return ifs; // match OpenJDK behavior: if malloc fails just return the unmodified netif
            }
            memcpy(addrP.brdcast.cast(), ifr_broadaddrP.cast(), addr_size);
        } else {
            addrP.brdcast = word(0);
        }

        // Deal with virtual interface with colon notation e.g. eth0:1
        int colonIdx = 0;
        for (int i=0; i<IF_NAMESIZE.intValue(); i++) {
            if (name[i].intValue() == word(':').intValue()) {
                colonIdx = i;
                break;
            }
        }
        if (colonIdx != 0) {
            c_int flags = auto(word(0));
            // This is a virtual interface. If we are able to access the parent
            // we need to create a new entry if it doesn't exist yet *and* update
            // the 'parent' interface with the new records.
            name[colonIdx] = word('\0');
            if (getFlags(sock, addr_of(name[0]), addr_of(flags)).intValue() < 0 || flags.intValue() < 0) {
                // failed to access parent interface do not create parent.
                // We are a virtual interface with no parent.
                isVirtual = true;
                name[colonIdx] = word(':');
            } else {
                // Got access to parent, so create it if necessary.
                // Save original name to vname and truncate name by ':'
                memcpy(addr_of(vname[0]).cast(), addr_of(name[0]).cast(), IF_NAMESIZE.cast());
                vname[colonIdx] = word(':');
            }
        }

        // Check if this is a "new" interface. Use the interface name for
        // matching because index isn't supported on Solaris 2.6 & 7.
        while (currif != null) {
            if (strcmp(addr_of(name[0]), currif.name.cast()).intValue() == 0) {
                break;
            }
            currif = currif.next;
        }

        // If "new" then create a netif instance and insert it into the list.
        if (currif == null) {
            currif = new netif();
            currif.name = malloc(IF_NAMESIZE.cast());
            strncpy(currif.name, addr_of(name[0]).cast(), IF_NAMESIZE.cast());
            currif.name.asArray()[IF_NAMESIZE.intValue() - 1] = word('\0');
            currif.index = getIndex(sock, addr_of(name[0]));
            currif.addr = null;
            currif.childs = null;
            currif.virtual = isVirtual;
            currif.next = ifs;
            ifs = currif;
        }

        // Finally insert the address on the interface
        addrP.next = currif.addr;
        currif.addr = addrP;

        netif parent = currif;

        // Deal with the virtual interface now.
        if (vname[0].intValue() != 0) {
            currif = parent.childs;
            while (currif != null) {
                if (strcmp(addr_of(vname[0]), currif.name.cast()).intValue() == 0) {
                    break;
                }
                currif = currif.next;
            }

            if (currif == null) {
                currif = new netif();
                currif.name = malloc(IF_NAMESIZE.cast());
                strncpy(currif.name, addr_of(vname[0]).cast(), IF_NAMESIZE.cast());
                currif.name.asArray()[IF_NAMESIZE.intValue() - 1] = word('\0');
                currif.index = getIndex(sock, addr_of(vname[0]));
                currif.addr = null; // Need to duplicate the addr entry?
                currif.virtual = true;
                currif.childs = null;
                currif.next = parent.childs;
                parent.childs = currif;
            }

            netaddr tmpaddr = new netaddr();
            tmpaddr.mask = addrP.mask;
            tmpaddr.family = addrP.family;
            tmpaddr.addr = malloc(addr_size);
            if (tmpaddr.addr.isNull()) {
                return ifs; // match OpenJDK behavior: if malloc fails bailout
            }
            memcpy(tmpaddr.addr.cast(), addrP.addr.cast(), addr_size);
            if (!addrP.brdcast.isNull()) {
                tmpaddr.brdcast = malloc(addr_size);
                if (tmpaddr.brdcast.isNull()) {
                    return ifs;// match OpenJDK behavior: if malloc fails bailout
                }
                memcpy(tmpaddr.brdcast.cast(), addrP.brdcast.cast(), addr_size);
            }

            tmpaddr.next = currif.addr;
            currif.addr = tmpaddr;
        }

        return ifs;
    }

    private static c_short translateIPv4AddressToPrefix(struct_sockaddr_in_ptr addr) {
        if (addr.isNull()) {
            return word(0);
        }
        short prefix = 0;
        int mask = ntohl(addr.sel().sin_addr.s_addr.cast()).intValue();
        while (mask != 0) {
            mask = mask << 1;
            prefix++;
        }
        return word(prefix);
    }

    private static c_int openSocket(c_int proto) throws SocketException {
        c_int sock = socket(proto, SOCK_DGRAM, word(0));
        if (sock.intValue() < 0) {
            if (errno != EPROTONOTSUPPORT.intValue() && errno != EAFNOSUPPORT.intValue()) {
                throw new SocketException("Socket creation failed");
            }
            return word(-1);
        }
        return sock;
    }

    private static netif enumIPv4Interfaces(c_int sock, netif ifs) throws SocketException {
        if (Build.Target.isLinux()) {
            struct_ifconf ifc = auto();

            // do a dummy SIOCGIFCONF to determine the buffer size
            // SIOCGIFCOUNT doesn't work
            if (ioctl(sock, SIOCGIFCONF, addr_of(ifc.ifc_len)).intValue() < 0) {
                throw new SocketException("ioctl(SIOCGIFCONF) failed");
            }

            // call SIOCGIFCONF to enumerate the interfaces
            char_ptr buf = malloc(word(ifc.ifc_len.intValue()));
            if (buf.isNull()) {
                throw new OutOfMemoryError("Native heap allocation failed");
            }
            try {
                ifc.ifc_ifcu = buf.cast();
                if (ioctl(sock, SIOCGIFCONF, addr_of(ifc)).intValue() < 0) {
                    throw new SocketException("ioctl(SIOCGIFCONF) failed");
                }

                // iterate through each interface
                struct_ifreq_ptr ifreqP = ifc.ifc_ifcu.cast(struct_ifreq_ptr.class);
                for (int i = 0; i < ifc.ifc_len.intValue() / sizeof(struct_ifreq.class).intValue(); i++, ifreqP = ifreqP.plus(1)) {
                    struct_sockaddr addr = auto();
                    struct_sockaddr broadaddr = auto();
                    ptr<struct_sockaddr> broadaddrP = auto();
                    c_short prefix = word(0);

                    // ignore non IPv4 addresses
                    if (addr_of(ifreqP.sel().ifr_ifru).loadUnshared(struct_sockaddr.class).sa_family != AF_INET.cast()) {
                        continue;
                    }

                    // save socket address
                    memcpy(addr_of(addr).cast(), addr_of(ifreqP.sel().ifr_ifru).cast(), sizeof(struct_sockaddr.class));

                    // determine broadcast address, if applicable
                    if ((ioctl(sock, SIOCGIFFLAGS, ifreqP).intValue() == 0) && ((addr_of(ifreqP.sel().ifr_ifru).loadUnshared(c_short.class).intValue() & IFF_BROADCAST.intValue()) != 0)) {
                        // restore socket address to ifreqP
                        memcpy(addr_of(ifreqP.sel().ifr_ifru).cast(), addr_of(addr).cast(), sizeof(struct_sockaddr.class));

                        if (ioctl(sock, SIOCGIFBRDADDR, ifreqP).intValue() == 0) {
                            memcpy(addr_of(broadaddr).cast(), addr_of(ifreqP.sel().ifr_ifru).cast(), sizeof(struct_sockaddr.class));
                            broadaddrP = addr_of(broadaddr);
                        }
                    }

                    // restore socket address to ifreqP
                    memcpy(addr_of(ifreqP.sel().ifr_ifru).cast(), addr_of(addr).cast(), sizeof(struct_sockaddr.class));

                    // determine netmask
                    if (ioctl(sock, SIOCGIFNETMASK, ifreqP).intValue() == 0) {
                        prefix = translateIPv4AddressToPrefix(addr_of(ifreqP.sel().ifr_ifru).cast(struct_sockaddr_in_ptr.class));
                    }

                    // add interface to the list
                    ifs = addif(sock, addr_of(ifreqP.sel().ifr_name[0]), ifs, addr_of(addr), broadaddrP, AF_INET, prefix);
                }
            } finally {
                free(buf);
            }
        } else if (Build.Target.isMacOs()) {
            struct_ifaddrs_ptr origifa = auto();

            if (getifaddrs(addr_of(origifa)).intValue() != 0) {
                throw new SocketException("getifaddrs() failed");
            }

            try {
                for (struct_ifaddrs_ptr ifa = origifa; !ifa.isNull(); ifa = addr_of(ifa.sel().ifa_next).loadUnshared().cast()) {
                    ptr<struct_sockaddr> broadaddrP = word(0);

                    // ignore non IPv4 addresses
                    if (addr_of(ifa.sel().ifa_addr).loadUnshared().isNull() ||
                            addr_of(addr_of(ifa.sel().ifa_addr).loadUnshared().sel().sa_family).loadUnshared().intValue() != AF_INET.intValue()) {
                        continue;
                    }

                    // set ifa_broadaddr, if there is one
                    if ((addr_of(ifa.sel().ifa_flags).loadUnshared().intValue() & IFF_POINTOPOINT.intValue()) == 0 &&
                            (addr_of(ifa.sel().ifa_flags).loadUnshared().intValue() & IFF_BROADCAST.intValue()) != 0) {
                        broadaddrP = addr_of(ifa.sel().ifa_dstaddr).loadUnshared();
                    }

                    ifs = addif(sock, addr_of(ifa.sel().ifa_name).loadUnshared().cast(), ifs, addr_of(ifa.sel().ifa_addr).loadUnshared(),
                            broadaddrP, AF_INET, translateIPv4AddressToPrefix(addr_of(ifa.sel().ifa_netmask).loadUnshared().cast()));
                }
            } finally {
                freeifaddrs(origifa);
            }
        }

        return ifs;
    }

    private static netif enumIPv6Interfaces(c_int sock, netif ifs) throws SocketException {
        // TODO: implement me -- currently not reachable: ipv6_available is hardwired to return false.
        return ifs;
    }

    private static c_int getIndex(c_int sock, const_char_ptr name) {
        if (Build.Target.isMacOs()) {
            unsigned_int index = if_nametoindex(name);
            return (index.intValue() == 0) ? word(-1) : index.cast();
        } else {
            struct_ifreq if2 = auto();
            strncpy(addr_of(if2.ifr_name[0]), name, word(IF_NAMESIZE.intValue() - 1));
            if (ioctl(sock, SIOCGIFINDEX, addr_of(if2)).intValue() <0){
                return word(-1);
            }
            return addr_of(addr_of(if2).sel().ifr_ifru).loadUnshared(c_int.class);
        }
    }

    private static c_int getFlags(c_int sock, const_char_ptr ifname, ptr<c_int> flags) {
        struct_ifreq if2 = auto();
        strncpy(addr_of(if2.ifr_name[0]), ifname, word(IF_NAMESIZE.intValue() - 1));

        if (ioctl(sock, SIOCGIFFLAGS, addr_of(if2)).intValue() < 0) {
            return word(-1);
        }
        // TODO: The OpenJDK code has a sizeof test on the ifreq.ifru.ifru_flags field to
        //       see if it has a size of 16 bits or not.  This was hard to translate, so for now we're
        //       just assuming that it is a c_short and is thus 16 bits.
        c_short f2 = addr_of(addr_of(if2).sel().ifr_ifru).loadUnshared(c_short.class);
        flags.storeUnshared(word(f2.intValue() & 0xffff));
        return word(0);
    }

    /*
     * Creates a NetworkInterface object, populates the name, the index, and
     * populates the InetAddress array based on the IP addresses for this
     * interface.
     */
    private static NetworkInterface createNetworkInterface(netif ifs) {
        // create a NetworkInterface object and populate it
        NetworkInterface$_patch netifObj = new NetworkInterface$_patch();
        String name = utf8zToJavaString(ifs.name.cast());
        netifObj.name = name;
        netifObj.displayName = name;
        netifObj.index = ifs.index.intValue();
        netifObj.virtual = ifs.virtual;

        // count the number of addresses on this interface
        int addr_count = 0;
        for (netaddr addrP = ifs.addr; addrP != null; addrP = addrP.next) {
            addr_count++;
        }

        InetAddress[] addrArr = new InetAddress[addr_count];
        InterfaceAddress[] bindArr = new InterfaceAddress[addr_count];
        int addr_index = 0;
        int bind_index = 0;
        for (netaddr addrP = ifs.addr; addrP != null; addrP = addrP.next) {
            InetAddress iaObj = null;
            if (addrP.family == AF_INET) {
                iaObj = new Inet4Address();
                unsigned_int tmpAddr = htonl(addr_of(addrP.addr.cast(struct_sockaddr_in_ptr.class).sel().sin_addr).loadUnshared(struct_in_addr.class).s_addr.cast()).cast();
                iaObj.holder().address = tmpAddr.intValue();
                InterfaceAddress ibObj = new InterfaceAddress();
                ((InterfaceAddress$_patch)(Object)ibObj).address = iaObj;
                if (!addrP.brdcast.isNull()) {
                    Inet4Address ia2Obj = new Inet4Address();
                    unsigned_int tmpBAddr = htonl(addr_of(addrP.brdcast.cast(struct_sockaddr_in_ptr.class).sel().sin_addr).loadUnshared(struct_in_addr.class).s_addr.cast()).cast();
                    ia2Obj.holder().address = tmpBAddr.intValue();
                    ((InterfaceAddress$_patch)(Object)ibObj).broadcast = ia2Obj;
                }
                ((InterfaceAddress$_patch)(Object)ibObj).maskLength = addrP.mask.shortValue();
                bindArr[bind_index++] = ibObj;
            }
            if (addrP.family == AF_INET6) {
                iaObj = new Inet6Address();
                Inet6Address$_patch ia6Obj = (Inet6Address$_patch)(Object)iaObj;
                ptr<uint8_t> addr = addr_of(addr_of(addrP.addr.cast(struct_sockaddr_in6_ptr.class).sel().sin6_addr).sel().s6_addr[0]);
                ia6Obj.setInet6Address_ipaddress(addr);
                uint32_t scope = addr_of(addrP.addr.cast(struct_sockaddr_in6_ptr.class).sel().sin6_scope_id).loadUnshared(uint32_t.class);
                if (scope.intValue() != 0) {
                    ia6Obj.setInet6Address_scopeid(scope.intValue());
                    ia6Obj.setInet6Address_scope_ifname((NetworkInterface)(Object)netifObj);
                }
                InterfaceAddress ibObj = new InterfaceAddress();
                ((InterfaceAddress$_patch)(Object)ibObj).address = iaObj;
                ((InterfaceAddress$_patch)(Object)ibObj).maskLength = addrP.mask.shortValue();
                bindArr[bind_index++] = ibObj;
            }
            addrArr[addr_index++] = iaObj;

        }

        // see if there is any virtual interface attached to this one.
        int childCount = 0;
        netif childP = ifs.childs;
        while (childP != null) {
            childCount++;
            childP = childP.next;
        }
        NetworkInterface[] childArr = new NetworkInterface[childCount];

        // create the NetworkInterface instances for the sub-interfaces as well
        int childIndex = 0;
        childP = ifs.childs;
        while(childP != null) {
            NetworkInterface tmp = createNetworkInterface(childP);
            if (tmp == null) {
                return null;
            }
            ((NetworkInterface$_patch)(Object)tmp).parent = (NetworkInterface)(Object)netifObj;
            childArr[childIndex++] = tmp;
            childP = childP.next;
        }

        netifObj.addrs = addrArr;
        netifObj.bindings = bindArr;
        netifObj.childs = childArr;

        return (NetworkInterface)(Object)netifObj;
    }

    static netif enumInterfaces() {
        netif ifs = null;
        try {
            c_int sock = openSocket(AF_INET);
            if (sock.intValue() >= 0) {
                ifs = enumIPv4Interfaces(sock, ifs);
                close(sock);
            }

            // If IPv6 is available then enumerate IPv6 addresses.
            // User can disable ipv6 explicitly by -Djava.net.preferIPv4Stack=true,
            // so we have to call ipv6_available()
            if (NetUtil.ipv6_available()) {
                sock = openSocket(AF_INET6);
                if (sock.intValue() < 0) {
                    freeif(ifs);
                    return null;
                }
                ifs = enumIPv6Interfaces(sock, ifs);
                close(sock);
            }
        } catch (SocketException e) {
            freeif(ifs);
            return null;
        }

        return ifs;
    }

    static NetworkInterface[] getAll() throws SocketException {
        netif ifs = enumInterfaces();
        if (ifs == null) {
            return null;
        }

        int ifCount = 0;
        netif curr = ifs;
        while (curr != null) {
            ifCount ++;
            curr = curr.next;
        }
        NetworkInterface[] netIFArr = new NetworkInterface[ifCount];

        curr = ifs;
        int arr_index = 0;
        while (curr != null) {
            NetworkInterface tmp = createNetworkInterface(curr);
            if (tmp == null) {
                freeif(ifs);
                return null;
            }
            netIFArr[arr_index++] = tmp;
            curr = curr.next;
        }

        freeif(ifs);
        return netIFArr;
    }
}
