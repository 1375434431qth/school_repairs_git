import { defineStore } from 'pinia'
import * as baseApi from '@/api/basedata'

export const useDictStore = defineStore('dict', {
  state: () => ({
    buildings: [],
    faultTypes: [],
    roomsMap: {},
    buildingLoaded: false,
    faultLoaded: false
  }),
  getters: {
    /** 故障类型平铺（两级），供下拉选择 */
    faultOptions: (state) => {
      const options = []
      state.faultTypes.forEach((parent) => {
        options.push({ value: parent.id, label: parent.name, children: [] })
        ;(parent.children || []).forEach((child) => {
          options[options.length - 1].children.push({ value: child.id, label: child.name })
        })
      })
      return options
    }
  },
  actions: {
    async loadBuildings(force = false) {
      if (this.buildingLoaded && !force) return this.buildings
      this.buildings = (await baseApi.listBuildings()) || []
      this.buildingLoaded = true
      return this.buildings
    },
    async loadFaultTypes(force = false) {
      if (this.faultLoaded && !force) return this.faultTypes
      this.faultTypes = (await baseApi.listFaultTypes()) || []
      this.faultLoaded = true
      return this.faultTypes
    },
    async roomsOf(buildingId, force = false) {
      if (!buildingId) return []
      if (this.roomsMap[buildingId] && !force) return this.roomsMap[buildingId]
      const rooms = (await baseApi.listRooms(buildingId)) || []
      this.roomsMap[buildingId] = rooms
      return rooms
    },
    invalidate() {
      this.buildingLoaded = false
      this.faultLoaded = false
      this.roomsMap = {}
    }
  }
})
