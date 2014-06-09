#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import random


class DiGraph:
    INFINITY = 10000
    UNDEFINDED = None
    _name = "graph"
    _data = {}

    def __init__(self, name=None):
        if name:
            self._name = name
        return

    def __getitem__(self, node):
        if self._data.has_key(node):
            return self._data[node]
        else:
            return None

    def __iter__(self):
        return self._data.__iter__()

    def add_node(self, node):
        if self._data.has_key(node):
            return False
        self._data[node] = {}
        return True

    def add_edge(self, node_from, node_to, cost=None):
        if not cost:
            cost = random.randrange(1, 11)
        self.add_node(node_from)
        self.add_node(node_to)
        self._data[node_from][node_to] = cost
        return

    def remove_edge(self, node_from, node_to, cost=None):
        if not self._data.has_key(node_from):
            return -1
        if self._data[node_from].has_key(node_to):
            if not cost:
                cost = self._data[node_from][node_to]
                if cost == self.INFINITY:
                    return -1
                else:
                    self._data[node_from][node_to] = self.INFINITY
                    return cost
            elif self._data[node_from][node_to] == cost:
                self._data[node_from][node_to] = self.INFINITY
                return cost
            else:
                return -1
        else:
            return -1

    def set_name(self, name):
        if name:
            self._name = name
        
            return True
        else:
            return False
    
