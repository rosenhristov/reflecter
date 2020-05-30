package com.company.projects.services;

import com.company.projects.annotations.IAmService;
import com.company.projects.annotations.INeedThis;

@IAmService
public class Address implements IService {

    @INeedThis
    private Info info;

    public Address() {
        announceInstantiation();
    }

    public Address(Info info) {
        this.info = info;
        announceInstantiation();
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public void announceInstantiation() {
        System.out.printf("%s is instantiated.\n", this.toString());
    }

    @Override
    public void print() {
        System.out.println(this.toString());
    }

    @Override
    public void printDeep() {
        print();
        if (info != null) info.printDeep();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
